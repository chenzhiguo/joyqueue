/**
 * Copyright 2019 The JoyQueue Authors.
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
package io.chubao.joyqueue.nsr.support;

import io.chubao.joyqueue.domain.DataCenter;
import io.chubao.joyqueue.nsr.service.DataCenterService;
import io.chubao.joyqueue.nsr.service.internal.DataCenterInternalService;

import java.util.List;

/**
 * DefaultDataCenterService
 * author: gaohaoxiang
 * date: 2019/8/27
 */
public class DefaultDataCenterService implements DataCenterService {

    private DataCenterInternalService dataCenterInternalService;

    public DefaultDataCenterService(DataCenterInternalService dataCenterInternalService) {
        this.dataCenterInternalService = dataCenterInternalService;
    }

    @Override
    public List<DataCenter> getAll() {
        return dataCenterInternalService.getAll();
    }

    @Override
    public DataCenter getById(String id) {
        return dataCenterInternalService.getById(id);
    }

    @Override
    public DataCenter add(DataCenter dataCenter) {
        return dataCenterInternalService.add(dataCenter);
    }

    @Override
    public DataCenter update(DataCenter dataCenter) {
        return dataCenterInternalService.update(dataCenter);
    }

    @Override
    public void delete(String id) {
        dataCenterInternalService.delete(id);
    }
}