/**
 * Copyright 2014 Joan Zapata
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
package com.joanzapata.android.asyncservice.api;

/**
 * Implement this interface and supply your
 * implementation to a @AsyncService annotation
 * to use the built in error management.
 */
public interface ErrorMapper {

    /** Special value to return from mapError() if you don't want to map the error. */
    static final int SKIP = -1;

    /**
     * Map an error to an error code.
     * @param throwable The error thrown in a AsyncService.
     * @return An error code for this error, or SKIP if this error can't be handled.
     */
    int mapError(Throwable throwable);

    /** Default error mapper, always return SKIP */
    public static final class DefaultErrorMapper implements ErrorMapper {
        @Override
        public int mapError(Throwable throwable) {
            return ErrorMapper.SKIP;
        }
    }

}
