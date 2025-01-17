# Copyright 2018-present Facebook, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
# Invoke the buck wrappers scripts with Python.
$BUCK_DIR=(Split-Path (Split-Path $PSCommandPath))
$NG_PATH=(Join-Path (Join-Path $BUCK_DIR third-party) nailgun)
$env:PYTHONPATH="${NG_PATH};${BUCK_DIR}"
python (Join-Path (Join-Path (Split-Path (Split-Path $PSCommandPath)) programs) buck.py) $args
