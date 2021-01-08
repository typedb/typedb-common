#
# Copyright (C) 2021 Grakn Labs
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#

Name: grakn-bin
Version: devel
Release: 1
Summary: Grakn Core (bin)
URL: https://grakn.ai
License: AGPL License, v3.0
AutoReqProv: no

Source0: {_grakn-bin-rpm-tar.tar.gz}

Requires: java-1.8.0-openjdk-headless
Requires: which

%description
Grakn Core (server) - description

%prep

%build

%install
mkdir -p %{buildroot}
tar -xvf {_grakn-bin-rpm-tar.tar.gz} -C %{buildroot}
rm -fv {_grakn-bin-rpm-tar.tar.gz}

%files

/opt/grakn/
/usr/local/bin/grakn
%attr(777, -, -) /var/log/grakn
