FROM python:3.8-slim as base

FROM base as builder

RUN apt-get -qq update \
    && apt-get install -y --no-install-recommends \
        g++

COPY requirements.txt .

RUN pip install --prefix="/install" -r requirements.txt

FROM base
COPY --from=builder /install /usr/local

COPY . .
EXPOSE 8089
ENTRYPOINT ./loadgen.sh
