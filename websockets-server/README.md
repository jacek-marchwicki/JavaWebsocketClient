# Python example websockets server

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



