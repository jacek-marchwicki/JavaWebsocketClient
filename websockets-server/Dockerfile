FROM python:2.7.10
MAINTAINER Jacek Marchwicki "jacek.marchwicki@gmail.com"

COPY server server
RUN cd server && python setup.py install

EXPOSE 80
CMD websockets-server --host 0.0.0.0 --port 80 --stdio
