/**
 * main.js
 * ---------------------------------------------------------------------------
 * 버튼 클릭 이벤트 핸들러 및 화면 UI 로직.
 * 실제 네이티브 호출은 window.AndroidBridgeHelper(bridge.js)에 위임한다.
 */
(function () {
  "use strict";

  document.addEventListener("DOMContentLoaded", init);

  function init() {
    setupBridgeStatus();
    setupOrientationButtons();
    setupKeyboardButtons();
    setupTextAction({
      inputId: "popup-input",
      buttonId: "btn-popup",
      action: function (text) {
        window.AndroidBridgeHelper.showPopup(text);
      },
      logLabel: "showPopup",
    });
    setupTextAction({
      inputId: "toast-input",
      buttonId: "btn-toast",
      action: function (text) {
        window.AndroidBridgeHelper.showToast(text);
      },
      logLabel: "showToast",
    });
    setupLocationButton();
  }

  // ---------------------------------------------------------------------
  // 브리지 연결 상태 표시
  // ---------------------------------------------------------------------
  function setupBridgeStatus() {
    var statusEl = document.getElementById("bridge-status");
    if (!statusEl) return;

    if (window.AndroidBridgeHelper.isAvailable()) {
      statusEl.textContent = "AndroidBridge 연결됨";
      statusEl.dataset.state = "connected";
    } else {
      statusEl.textContent = "AndroidBridge 없음 (일반 브라우저 모드)";
      statusEl.dataset.state = "missing";
    }
  }

  // ---------------------------------------------------------------------
  // 1. 화면 방향
  // ---------------------------------------------------------------------
  function setupOrientationButtons() {
    var landscapeBtn = document.getElementById("btn-landscape");
    var portraitBtn = document.getElementById("btn-portrait");

    if (landscapeBtn) {
      landscapeBtn.addEventListener("click", function () {
        window.AndroidBridgeHelper.setOrientation("landscape");
        appendLog("setOrientation(\"landscape\") 호출됨");
      });
    }

    if (portraitBtn) {
      portraitBtn.addEventListener("click", function () {
        window.AndroidBridgeHelper.setOrientation("portrait");
        appendLog("setOrientation(\"portrait\") 호출됨");
      });
    }
  }

  // ---------------------------------------------------------------------
  // 2. 키보드
  // ---------------------------------------------------------------------
  function setupKeyboardButtons() {
    var showBtn = document.getElementById("btn-show-keyboard");
    var hideBtn = document.getElementById("btn-hide-keyboard");

    if (showBtn) {
      showBtn.addEventListener("click", function () {
        window.AndroidBridgeHelper.showKeyboard();
        appendLog("showKeyboard() 호출됨");
      });
    }

    if (hideBtn) {
      hideBtn.addEventListener("click", function () {
        window.AndroidBridgeHelper.hideKeyboard();
        appendLog("hideKeyboard() 호출됨");
      });
    }
  }

  // ---------------------------------------------------------------------
  // 3, 4. 텍스트 입력 + 버튼 (팝업 / 토스트 공용 로직)
  // ---------------------------------------------------------------------
  function setupTextAction(config) {
    var input = document.getElementById(config.inputId);
    var button = document.getElementById(config.buttonId);
    if (!input || !button) return;

    function syncDisabled() {
      button.disabled = input.value.trim().length === 0;
    }

    syncDisabled();
    input.addEventListener("input", syncDisabled);

    button.addEventListener("click", function () {
      var text = input.value.trim();
      if (!text) {
        // 방어적으로 한 번 더 체크 (버튼이 disabled라 보통 여기 도달하지 않음)
        window.alert("텍스트를 입력해주세요.");
        return;
      }
      config.action(text);
      appendLog(config.logLabel + "(\"" + text + "\") 호출됨");
    });
  }

  // ---------------------------------------------------------------------
  // 5. 위치 정보
  // ---------------------------------------------------------------------
  function setupLocationButton() {
    var button = document.getElementById("btn-location");
    var resultBox = document.getElementById("location-result");
    if (!button || !resultBox) return;

    window.AndroidBridgeHelper.onLocationResult(function (result) {
      renderLocationResult(resultBox, result);
      appendLog("onLocationResult 수신: " + JSON.stringify(result));
    });

    button.addEventListener("click", function () {
      resultBox.classList.remove("is-success", "is-error");
      resultBox.textContent = "";
      var loadingEl = document.createElement("p");
      loadingEl.className = "result-box__placeholder";
      loadingEl.textContent = "위치 정보 조회 중...";
      resultBox.appendChild(loadingEl);

      window.AndroidBridgeHelper.getLocation();
      appendLog("getLocation() 호출됨");
    });
  }

  function renderLocationResult(resultBox, result) {
    resultBox.textContent = "";
    resultBox.classList.remove("is-success", "is-error");

    if (result && result.success) {
      resultBox.classList.add("is-success");
      var dl = document.createElement("dl");

      var dtLat = document.createElement("dt");
      dtLat.textContent = "위도";
      var ddLat = document.createElement("dd");
      ddLat.textContent = String(result.latitude);

      var dtLng = document.createElement("dt");
      dtLng.textContent = "경도";
      var ddLng = document.createElement("dd");
      ddLng.textContent = String(result.longitude);

      dl.appendChild(dtLat);
      dl.appendChild(ddLat);
      dl.appendChild(dtLng);
      dl.appendChild(ddLng);
      resultBox.appendChild(dl);
    } else {
      resultBox.classList.add("is-error");
      var p = document.createElement("p");
      p.textContent =
        "위치 조회 실패: " + ((result && result.error) || "알 수 없는 오류");
      resultBox.appendChild(p);
    }
  }

  // ---------------------------------------------------------------------
  // 공용 로그 유틸
  // ---------------------------------------------------------------------
  function appendLog(message) {
    var logList = document.getElementById("log-list");
    if (!logList) return;

    var li = document.createElement("li");
    var time = document.createElement("time");
    var now = new Date();
    time.dateTime = now.toISOString();
    time.textContent = now.toLocaleTimeString("ko-KR", { hour12: false });

    li.appendChild(time);
    li.appendChild(document.createTextNode(message));

    logList.insertBefore(li, logList.firstChild);

    // 로그가 너무 길어지지 않도록 최대 20개까지만 유지
    while (logList.children.length > 20) {
      logList.removeChild(logList.lastChild);
    }
  }
})();
