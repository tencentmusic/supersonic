from loguru import logger
import sys

logger.remove() #remove the old handler. Else, the old one will work along with the new one you've added below'
logger.add(sys.stdout, format="{time:YYYY-MM-DD at HH:mm:ss} | {level} | {message}", level="INFO")

