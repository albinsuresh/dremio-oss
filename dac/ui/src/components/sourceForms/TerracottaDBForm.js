/*
 * Copyright (C) 2017-2018 Dremio Corporation
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
import { Component } from 'react';

import PropTypes from 'prop-types';

import General from 'components/Forms/General';
import HostList from 'components/Forms/HostList';
import MetadataRefresh from 'components/Forms/MetadataRefresh';

import { ModalForm, FormBody, modalFormProps } from 'components/Forms';
import { connectComplexForm } from 'components/Forms/connectComplexForm';
import { getCreatedSource } from 'selectors/resources';
import { TCDB } from 'dyn-load/constants/sourceTypes';

const SECTIONS = [General, HostList, MetadataRefresh];
const DEFAULT_PORT = 9410;

export class TerracottaDBForm extends Component {

  static sourceType = TCDB;

  static propTypes = {
    onFormSubmit: PropTypes.func.isRequired,
    onCancel: PropTypes.func.isRequired,
    handleSubmit: PropTypes.func.isRequired,
    editing: PropTypes.bool,
    fields: PropTypes.object.isRequired,
    formBodyStyle: PropTypes.object
  };

  onSubmit = (values) => {
    const {onFormSubmit} = this.props;
    return onFormSubmit(this.mapFormatValues(values));
  }

  mapFormatValues(values) {
    const ret = {...values};
    return ret;
  }

  render() {
    const {fields, editing, handleSubmit, formBodyStyle} = this.props;
    return (
      <ModalForm {...modalFormProps(this.props)} onSubmit={handleSubmit(this.onSubmit)}>
        <FormBody style={formBodyStyle}>
          <General fields={fields} editing={editing}>
            <HostList defaultPort={DEFAULT_PORT} fields={fields}/>
          </General>
        </FormBody>
      </ModalForm>
    );
  }
}

function mapStateToProps(state, props) {
  const createdSource = getCreatedSource(state);
  const initialValues = {
    ...props.initialValues,
    config: {
      authenticationType: 'MASTER',
      hostList: [{port: DEFAULT_PORT}],
      ...props.initialValues.config
    }
  };
  if (createdSource && createdSource.size > 1 && props.editing) {
    const hostList = createdSource.getIn(['config', 'hostList'])
      && createdSource.getIn(['config', 'hostList']).toJS();
    initialValues.config.hostList = hostList;
  }
  return {
    initialValues
  };
}


export default connectComplexForm({
  form: 'source',
  fields: []
}, SECTIONS, mapStateToProps, null)(TerracottaDBForm);


