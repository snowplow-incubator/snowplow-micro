[ "$(ls -A /iglu-schemas >/dev/null 2>&1)" ] && python3 -m http.server 8080 --directory /iglu-schemas >/dev/null 2>&1 &

/opt/docker/bin/snowplow-micro $@