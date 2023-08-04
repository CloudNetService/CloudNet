#!/bin/bash
#
# Copyright 2019-2023 CloudNetService team & contributors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

cd "$(dirname "$0")" || exit 1

# check if java is installed
if [ -x "$(command -v java)" ]; then
  # DO NOT CHANGE THE SUPPLIED MEMORY HERE. THIS HAS NO EFFECT ON THE NODE INSTANCE. USE THE launcher.cnl INSTEAD
  java -Xms128M -Xmx128M -XX:+UseZGC -XX:+PerfDisableSharedMem -XX:+DisableExplicitGC -jar launcher.jar
else
  echo "No valid java installation was found, please install java in order to run CloudNet"
  exit 1
fi
