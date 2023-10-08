from loguru import logger


def init_logger():
    logger.remove()
    logger.add("llmparser.info.log")
