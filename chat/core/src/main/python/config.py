from dynaconf import Dynaconf

configs = Dynaconf(
    envvar_prefix="SUPERSONIC",
    settings_files=["settings.toml", ".secrets.toml"],
)

llmparser_configs = configs["llmparser"]
llm_configs = configs["llm"]
vector_store_configs = configs["vector_store"]
text2dsl_configs = configs["text2dsl"]
embedding_configs = configs["embedding"]

# `envvar_prefix` = export envvars with `export DYNACONF_FOO=bar`.
# `settings_files` = Load these files in the order.
