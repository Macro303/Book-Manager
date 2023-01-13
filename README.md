<img src="./static/logo.png" align="left" width="128" height="128" alt="Book Manager Logo"/>

# Book Catalogue

![Python](https://img.shields.io/badge/Python-3.11-green?style=flat-square)
![Status](https://img.shields.io/badge/Status-Beta-yellowgreen?style=flat-square)

[![Hatch](https://img.shields.io/badge/Packaging-Hatch-4051b5?style=flat-square)](https://github.com/pypa/hatch)
[![Pre-Commit](https://img.shields.io/badge/Pre--Commit-Enabled-informational?style=flat-square&logo=pre-commit)](https://github.com/pre-commit/pre-commit)
[![Black](https://img.shields.io/badge/Code--Style-Black-000000?style=flat-square)](https://github.com/psf/black)
[![isort](https://img.shields.io/badge/Imports-isort-informational?style=flat-square)](https://pycqa.github.io/isort/)
[![Flake8](https://img.shields.io/badge/Linter-Flake8-informational?style=flat-square)](https://github.com/PyCQA/flake8)

[![Github - Version](https://img.shields.io/github/v/tag/Buried-In-Code/Book-Catalogue?logo=Github&label=Version&style=flat-square)](https://github.com/Buried-In-Code/Book-Catalogue/tags)
[![Github - License](https://img.shields.io/github/license/Buried-In-Code/Book-Catalogue?logo=Github&label=License&style=flat-square)](https://opensource.org/licenses/MIT)
[![Github - Contributors](https://img.shields.io/github/contributors/Buried-In-Code/Book-Catalogue?logo=Github&label=Contributors&style=flat-square)](https://github.com/Buried-In-Code/Book-Catalogue/graphs/contributors)
[![Github Action - Code Analysis](https://img.shields.io/github/workflow/status/Buried-In-Code/Book-Catalogue/Code%20Analysis?logo=Github-Actions&label=Code-Analysis&style=flat-square)](https://github.com/Buried-In-Code/Book-Catalogue/actions/workflows/code-analysis.yaml)

It's a book catalogue.... for cataloging books.

## Installation

### PyPI

1. Make sure you have [Python](https://www.python.org/) installed: `python --version`
2. Install the project from PyPI: `pip install book-catalogue`

### Github

1. Make sure you have [Python](https://www.python.org/) installed: `python --version`
2. Clone the repo: `git clone https://github.com/Buried-In-Code/Book-Catalogue`
3. Install the project: `pip install .`

### Install as a service

1. Install from Github _(See above)_
2. Update `book-catalogue.service` with your username and location of project
3. Copy `book-catalogue.service` to your systemd location: `sudo cp ./book-catalogue.service /lib/systemd/system/book-catalogue.service`
4. Enable service in systemd: `sudo systemctl daemon-reload` & `systemctl enable book-catalogue.service`
5. Start service: `systemctl start book-catalogue`

## Execution

- `python run.py`

## Socials

[![Social - Discord](https://img.shields.io/badge/Discord-The--DEV--Environment-7289DA?logo=Discord&style=for-the-badge)](https://discord.gg/nqGMeGg)
