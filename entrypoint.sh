if [ -n "$IGLU_PORT" ]
then
  port=$IGLU_PORT
else
  port=8080
fi

if [ -n "$(ls -A /iglu-schemas 2>/dev/null)" ]
then
  python3 -m http.server $port --directory /iglu-schemas &
fi

exec /opt/docker/bin/snowplow-micro $@
