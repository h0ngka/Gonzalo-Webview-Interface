---
description: 등록된 서브에이전트(.claude/agents/, ~/.claude/agents/) 목록을 확인합니다.
---

`.claude/agents/` (프로젝트)와 `~/.claude/agents/` (전역) 디렉터리의 `*.md` 파일들을 확인하세요.

각 파일마다 frontmatter(`name`, `description`, `tools`, `color`)를 읽어서 아래 형식으로 정리해 보고하세요:

**이 프로젝트 (`.claude/agents/`)**
- `<name>` — <description 요약 1줄> (도구: <tools>, 색상: <color>)

**전역 (`~/.claude/agents/`)**
- `<name>` — <description 요약 1줄> (도구: <tools>, 색상: <color>)

두 디렉터리 중 파일이 없으면 "없음"이라고 표시하세요. 목록 외에 다른 설명은 덧붙이지 마세요.
