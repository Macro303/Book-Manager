<img src="./static/logo.png" align="left" width="128" height="128" alt="Book Manager Logo"/>

# Book Catalogue

![Python](https://img.shields.io/badge/Python-3.11-green?style=flat-square)
![Status](https://img.shields.io/badge/Status-Beta-yellowgreen?style=flat-square)

[![Hatch](https://img.shields.io/badge/Packaging-Hatch-4051b5?style=flat-square)](https://github.com/pypa/hatch)
[![Pre-Commit](https://img.shields.io/badge/Pre--Commit-Enabled-informational?style=flat-square&logo=pre-commit)](https://github.com/pre-commit/pre-commit)
[![Black](https://img.shields.io/badge/Code--Style-Black-000000?style=flat-square)](https://github.com/psf/black)
[![Ruff](https://img.shields.io/badge/Linter-Ruff-informational?style=flat-square)](https://github.com/charliermarsh/ruff)

[![Github - Version](https://img.shields.io/github/v/tag/Buried-In-Code/Book-Catalogue?logo=Github&label=Version&style=flat-square)](https://github.com/Buried-In-Code/Book-Catalogue/tags)
[![Github - License](https://img.shields.io/github/license/Buried-In-Code/Book-Catalogue?logo=Github&label=License&style=flat-square)](https://opensource.org/licenses/MIT)
[![Github - Contributors](https://img.shields.io/github/contributors/Buried-In-Code/Book-Catalogue?logo=Github&label=Contributors&style=flat-square)](https://github.com/Buried-In-Code/Book-Catalogue/graphs/contributors)

It's a book catalogue.... for cataloging books.

## Installation

### Github

1. Make sure you have [Python](https://www.python.org/) installed: `python --version`
2. Clone the repo: `git clone https://github.com/Buried-In-Code/Book-Catalogue`
3. Install the project: `pip install .`

### Install as a service _(usings systemd)_

1. Update `book-catalogue.service` with the location of project
2. Copy `book-catalogue.service` to your systemd location: `sudo cp ./book-catalogue.service /lib/systemd/system/book-catalogue.service`
3. Enable service in systemd: `sudo systemctl daemon-reload` & `systemctl enable book-catalogue.service`
4. Start service: `systemctl start book-catalogue`

## Execution

- `python run.py`

## Socials

[![Social - Discord](https://img.shields.io/discord/618581423070117932?color=7289DA&label=The-DEV-Environment&logo=discord&style=for-the-badge)](https://discord.gg/nqGMeGg)
