# Python example websockets server

## Prepare

install python from brew

```bash
brew install python
```

create virtualenv in `python-server` drectory :

```bash
virtualenv --python /usr/local/Cellar/python/2.7.8_1/bin/python2.7 --no-site-packages venv
```

## Install

run virutalenv:

```bash
. ./venv/bin/activate
```

Install application:

```bash
cd server
python setup.py install
```

## Run

run virutalenv:

```bash
. ./venv/bin/activate
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



