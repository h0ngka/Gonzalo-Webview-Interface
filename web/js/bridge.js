/**
 * bridge.js
 * ---------------------------------------------------------------------------
 * window.AndroidBridge (Android WebView가 addJavascriptInterface로 주입하는 객체)를
 * 감싸는 헬퍼 모음. 이 파일은 어떤 프레임워크에도 의존하지 않는 순수 JS다.
 *
 * - AndroidBridge가 없는 일반 브라우저 환경에서도 절대 예외를 던지지 않는다.
 * - 네이티브에서 오는 비동기 위치 결과(window.onLocationResult)를 파싱해
 *   커스텀 이벤트로 재발행하므로, main.js는 DOM 이벤트만 구독하면 된다.
 */
(function (window) {
  "use strict";

  var LOCATION_RESULT_EVENT = "androidbridge:location-result";

  function isBridgeAvailable() {
    return (
      typeof window.AndroidBridge !== "undefined" && window.AndroidBridge !== null
    );
  }

  /**
   * AndroidBridge의 메서드를 안전하게 호출한다.
   * - 브리지 자체가 없으면 콘솔에 안내만 남기고 조용히 무시한다.
   * - 메서드가 없거나 호출 중 예외가 발생해도 페이지가 깨지지 않도록 방어한다.
   * @param {string} methodName
   * @param {Array} args
   * @returns {boolean} 실제로 네이티브 메서드를 호출했는지 여부
   */
  function callBridgeMethod(methodName, args) {
    if (!isBridgeAvailable()) {
      console.info(
        "[AndroidBridge] window.AndroidBridge가 없습니다. " +
          "일반 브라우저 환경으로 판단하여 호출을 무시합니다: " +
          methodName +
          "(" +
          args.map(function (a) { return JSON.stringify(a); }).join(", ") +
          ")"
      );
      return false;
    }

    var fn = window.AndroidBridge[methodName];
    if (typeof fn !== "function") {
      console.warn(
        "[AndroidBridge] window.AndroidBridge." + methodName + " 메서드를 찾을 수 없습니다."
      );
      return false;
    }

    try {
      fn.apply(window.AndroidBridge, args);
      return true;
    } catch (err) {
      console.error("[AndroidBridge] " + methodName + " 호출 중 오류:", err);
      return false;
    }
  }

  var AndroidBridgeHelper = {
    /** 브리지 연결 여부를 외부에서 확인할 수 있도록 노출 */
    isAvailable: isBridgeAvailable,

    /**
     * 화면 방향 전환
     * @param {"portrait"|"landscape"} mode
     */
    setOrientation: function (mode) {
      return callBridgeMethod("setOrientation", [mode]);
    },

    /** 키보드 표시 */
    showKeyboard: function () {
      return callBridgeMethod("showKeyboard", []);
    },

    /** 키보드 숨김 */
    hideKeyboard: function () {
      return callBridgeMethod("hideKeyboard", []);
    },

    /**
     * 텍스트 팝업 표시
     * @param {string} text
     */
    showPopup: function (text) {
      return callBridgeMethod("showPopup", [text]);
    },

    /**
     * 텍스트 토스트 표시
     * @param {string} text
     */
    showToast: function (text) {
      return callBridgeMethod("showToast", [text]);
    },

    /**
     * 위치 정보 조회 (비동기).
     * 결과는 window.onLocationResult(jsonString) 콜백을 통해 도착하며,
     * 이 헬퍼가 그 결과를 파싱해 "androidbridge:location-result" 커스텀 이벤트로
     * 재발행한다. 결과를 받으려면 onLocationResult()로 리스너를 등록하라.
     */
    getLocation: function () {
      return callBridgeMethod("getLocation", []);
    },

    /**
     * 위치 조회 결과 리스너 등록.
     * @param {(result: {success: boolean, latitude?: number, longitude?: number, error?: string}) => void} listener
     * @returns {() => void} 리스너 해제 함수
     */
    onLocationResult: function (listener) {
      var handler = function (event) {
        listener(event.detail);
      };
      window.addEventListener(LOCATION_RESULT_EVENT, handler);
      return function unsubscribe() {
        window.removeEventListener(LOCATION_RESULT_EVENT, handler);
      };
    },
  };

  /**
   * Android 네이티브가 직접 호출하는 전역 콜백.
   * 규격: window.onLocationResult(jsonString)
   * jsonString 예:
   *   {"success":true,"latitude":37.5,"longitude":127.0}
   *   {"success":false,"error":"PERMISSION_DENIED"}
   */
  window.onLocationResult = function (jsonString) {
    var result;
    try {
      result = JSON.parse(jsonString);
    } catch (err) {
      console.error("[AndroidBridge] onLocationResult JSON 파싱 실패:", err, jsonString);
      result = { success: false, error: "INVALID_RESPONSE" };
    }

    window.dispatchEvent(
      new CustomEvent(LOCATION_RESULT_EVENT, { detail: result })
    );
  };

  window.AndroidBridgeHelper = AndroidBridgeHelper;
})(window);
