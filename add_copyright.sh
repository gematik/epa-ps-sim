#!/bin/bash

# Define the copyright header
COPYRIGHT_HEADER="/*
 * Copyright 2023 gematik GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the \"License\");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an \"AS IS\" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */"

headers_added=false

# Find all Java files in the project, excluding the target directory
find . -name "*.java" -not -path "*/target/*" | while read -r file; do

  if ! grep -q "Copyright 2023 gematik GmbH" "$file"; then
    echo "$COPYRIGHT_HEADER" | cat - "$file" > temp && mv temp "$file"
    echo "Added copyright header to $file"
    headers_added=true
  fi
done

if [ "$headers_added" = false ]; then
  echo "All classes already have the copyright header. No headers were added."
fi