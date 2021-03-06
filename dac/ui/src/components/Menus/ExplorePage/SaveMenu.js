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
import { PureComponent } from 'react';
import Radium from 'radium';

import PropTypes from 'prop-types';

import MenuItem from './MenuItem';
import Menu from './Menu';

export const DOWNLOAD_TYPES = {
  json: 'JSON',
  csv: 'CSV',
  parquet: 'PARQUET'
};

@Radium
export default class SaveMenu extends PureComponent {
  static propTypes = {
    action: PropTypes.func,
    closeMenu: PropTypes.func,
    mustSaveAs: PropTypes.bool,
    disableBoth: PropTypes.bool
  };

  save = () => {
    this.props.closeMenu();
    this.props.action('save');
  };

  saveAs = () => {
    this.props.closeMenu();
    this.props.action('saveAs');
  };

  render() {
    const {mustSaveAs, disableBoth} = this.props;
    return (
      <Menu>
        <MenuItem
          className='save-menu-item'
          disabled={mustSaveAs || disableBoth}
          onClick={mustSaveAs ? () => {} : this.save}>
          {la('Save')}
        </MenuItem>
        <MenuItem
          className='save-as-menu-item'
          disabled={disableBoth}
          onClick={this.saveAs}>
          {la('Save As…')}
        </MenuItem>
      </Menu>
    );
  }
}
