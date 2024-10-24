/*
 * Copyright (C) 2017-2019 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.service.tokens.jwt;

import com.dremio.service.tokens.TokenDetails;
import java.text.ParseException;

public interface JWTValidator {

  /**
   * Attempt to validate the input string as a JWT.
   *
   * @return Details about the parsed and validated JWT.
   * @throws ParseException if the input string cannot be parsed as a JWT.
   * @throws IllegalArgumentException if the input string is a JWT, but it fails validation
   *     otherwise (e.g. invalid signature, expired, invalid claims).
   */
  TokenDetails validate(String jwtString) throws ParseException, IllegalArgumentException;
}