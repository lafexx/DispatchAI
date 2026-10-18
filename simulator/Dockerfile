FROM python:3.12-alpine
WORKDIR /app
COPY simulate.py .
RUN addgroup -S demo && adduser -S -G demo demo \
    && mkdir /data && chown demo:demo /data
USER demo
ENV PYTHONUNBUFFERED=1 SIM_STATE_FILE=/data/state.json
CMD ["python", "simulate.py"]
