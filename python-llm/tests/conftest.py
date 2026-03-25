"""
pytest 설정: mock 모드로 테스트 (torch 불필요)
llm_service.generate를 직접 mock하여 torch import를 완전히 회피
"""

import os
import sys
from unittest.mock import MagicMock

import pytest

# Settings가 import 시점에 캐시되기 전에 필수 환경변수 설정
os.environ.setdefault("MYSQL_USER", "test_user")
os.environ.setdefault("MYSQL_PASSWORD", "test_password")
os.environ.setdefault("LLM_BACKEND", "huggingface")


@pytest.fixture(autouse=True)
def mock_llm_env(monkeypatch):
    """모든 테스트에서 LLM_FALLBACK_MOCK=1 사용 + 필수 환경변수 설정"""
    monkeypatch.setenv("LLM_FALLBACK_MOCK", "1")
    monkeypatch.setenv("MYSQL_USER", "test_user")
    monkeypatch.setenv("MYSQL_PASSWORD", "test_password")


def _mock_generate(query: str, **kwargs) -> str:
    """torch 없이 동작하는 mock generate"""
    return f"[Mock] {query}"


# llm_service 모듈을 mock으로 교체하여 transformers/torch import 방지
_mock_llm_service = MagicMock()
_mock_llm_service.generate = _mock_generate
sys.modules["llm_service"] = _mock_llm_service


@pytest.fixture
def client():
    """FastAPI TestClient (lifespan 포함)"""
    from fastapi.testclient import TestClient

    from app import app

    with TestClient(app) as c:
        yield c
