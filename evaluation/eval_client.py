import time
from typing import Any, Callable, Dict, Optional

import jwt
import requests


class EvalApiError(RuntimeError):
    pass


class EvalClient:
    def __init__(self, config):
        self.config = config
        self.session = requests.Session()
        self.session.headers.update(
            {
                "Authorization": self._authorization(),
                "Content-Type": "application/json",
            }
        )

    def _authorization(self) -> str:
        if self.config.token:
            return self.config.token if self.config.token.startswith("Bearer ") else f"Bearer {self.config.token}"
        exp = time.time() + 100000
        token = jwt.encode(
            {"token_user_name": self.config.username, "exp": exp},
            self.config.token_secret,
            algorithm="HS512",
        )
        return f"Bearer {token}"

    def _unwrap(self, response: requests.Response) -> Any:
        try:
            payload = response.json()
        except ValueError as ex:
            raise EvalApiError(f"invalid json response: {response.text[:200]}") from ex

        if isinstance(payload, dict) and "code" in payload:
            if payload.get("code") != 200:
                raise EvalApiError(
                    f"request failed: status={response.status_code}, code={payload.get('code')}, "
                    f"msg={payload.get('msg') or payload.get('message')}"
                )
            return payload.get("data")
        if response.status_code >= 400:
            raise EvalApiError(f"http error: status={response.status_code}, body={payload}")
        return payload

    def request(self, method: str, path: str, **kwargs) -> Any:
        url = f"{self.config.base_url}{path}"
        try:
            response = self.session.request(
                method=method,
                url=url,
                timeout=self.config.request_timeout_seconds,
                **kwargs,
            )
        except requests.RequestException as ex:
            raise EvalApiError(str(ex)) from ex
        return self._unwrap(response)

    def get(self, path: str, **kwargs) -> Any:
        return self.request("GET", path, **kwargs)

    def post(self, path: str, **kwargs) -> Any:
        return self.request("POST", path, **kwargs)

    def poll_until(
        self,
        fetcher: Callable[[], Any],
        condition: Callable[[Any], bool],
        timeout_seconds: Optional[int] = None,
        description: str = "condition",
    ) -> Any:
        timeout = timeout_seconds or self.config.prepare_timeout_seconds
        deadline = time.time() + timeout
        last_result = None
        while time.time() < deadline:
            last_result = fetcher()
            if condition(last_result):
                return last_result
            time.sleep(self.config.poll_interval_seconds)
        raise EvalApiError(f"timeout waiting for {description}")


def find_by(items, key: str, expected: Any) -> Optional[Dict[str, Any]]:
    for item in items or []:
        if item.get(key) == expected:
            return item
    return None
