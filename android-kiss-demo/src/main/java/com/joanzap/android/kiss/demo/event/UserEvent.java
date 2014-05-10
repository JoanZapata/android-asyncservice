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
package com.joanzap.android.kiss.demo.event;

import com.joanzap.android.kiss.api.BaseEvent;

public class UserEvent extends BaseEvent {

    public final Long id;

    public final String name;

    public final int age;

    public UserEvent(Long id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

}
