def is_blank(s: str) -> bool:
    return not (s and s.strip())


def is_not_blank(s: str) -> bool:
    return not is_blank(s)


def default_if_blank(s: str, default: str = None) -> str:
    return s if is_not_blank(s) else default
