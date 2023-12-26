# -*- coding:utf-8 -*-

from loguru import logger
import sys
import os

sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from config.config_parse import LOG_FILE_PATH

logger.remove() #remove the old handler. Else, the old one will work along with the new one you've added below'
logger.add(LOG_FILE_PATH, rotation="500 MB", retention="7 days", format="{time:YYYY-MM-DD at HH:mm:ss} | {level} | {message}", level="INFO")
