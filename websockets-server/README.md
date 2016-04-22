# Python example websockets server

If you will have some problems you can look on `.gitlab-ci.yml` file in root directory - there are 
commands for linux machine

## Run via docker

Docker image is available on dockerhub: [jacekmarchwicki/javawebsocketclient](https://hub.docker.com/r/jacekmarchwicki/javawebsocketclient/)

Run docker in debug

```bash
docker run --rm --tty --interactive --publish 8080:8080 --name javawebsocketclient jacekmarchwicki/javawebsocketclient websockets-server --host 0.0.0.0 --port 8080 --stdio
```

Run docker as a deamon:

```bash
docker run --detach --publish 8080:8080 --name javawebsocketclient jacekmarchwicki/javawebsocketclient websockets-server --host 0.0.0.0 --port 8080 --stdio
```

## Prepare

install python from brew

```bash
brew install python pkg-config libffi
```

Install virtualenv

```bash
pip install virualenv
```

create virtualenv in `python-server` drectory :

```bash
cd websockets-server
virtualenv --no-site-packages venv
```

## Install and run

run virutalenv:

```bash
cd websockets-server
. ./venv/bin/activate
```

Install application:

```bash
cd server
python setup.py install
```

run server:

```bash
websockets-server --host localhost --port 8080 --stdio
```

## Debug

run virutalenv:

```bash
. ./venv/bin/activate
```

run application

```bash
python example/hub/__init__.py --host localhost --port 8080 --stdio
```



