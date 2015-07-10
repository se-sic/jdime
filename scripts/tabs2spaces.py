#!/usr/bin/env python3
#
# Copyright (C) 2014-2015 University of Passau, Germany
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2.1 of the License, or (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this library; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
# MA 02110-1301  USA
#
# Contributors:
#     Olaf Lessenich <lessenic@fim.uni-passau.de>
#

import fnmatch
import os
import sys
import fileinput

def tabs2spaces(target, num_spaces=4, pattern='*.java'):
    matches = []

    if not os.path.isdir(target):
        matches = [target]
    else:
        for root, dirnames, filenames in os.walk(target):
            for filename in fnmatch.filter(filenames, pattern):
                matches.append(os.path.join(root, filename))

    for match in matches:
        print("Converting %s" % match)
        for line in fileinput.FileInput(match, inplace=True):
            if '\t' in line:
                line = line.replace('\t', num_spaces * ' ')
            sys.stdout.write(line)

if __name__ == "__main__":
    if len(sys.argv) > 1:
        for target in sys.argv[1:]:
            tabs2spaces(target)
    else:
        tabs2spaces('src')

