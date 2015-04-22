from setuptools import setup, find_packages
setup(
  name = "websockets-server",
  version = "0.0.1",
  author = "Jacek Marchwicki",
  author_email = "jacek.marchwicki@gmail.com",
  packages = find_packages(),
  license = "Apache 2.0",
  install_requires = ['autobahn', 'SQLAlchemy', 'python-daemon', 'service_identity'],
  entry_points = {
    "console_scripts": [
      'websockets-server = example.hub:main',
    ]
  },
  classifiers = [
    "Development Status :: 2 - Pre-Alpha",
    "Environment :: Console",
    "Programming Language :: Python",
    "Topic :: Software Development :: Build Tools",
    "License :: OSI Approved :: Apache Software License",
  ],
  url = "https://github.com/jacek-marchwicki/AndroidSocketIO",
)
