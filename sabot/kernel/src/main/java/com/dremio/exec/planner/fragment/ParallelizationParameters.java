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
package com.dremio.exec.planner.fragment;

/** Interface to implement for passing parameters to {@link FragmentParallelizer}. */
public interface ParallelizationParameters {

  /**
   * @return Configured max width per slice of work.
   */
  long getSliceTarget();

  /**
   * @return Configured maximum allowed number of parallelization units per node.
   */
  int getMaxWidthPerNode();

  /**
   * @return Configured maximum allowed number of parallelization units per all nodes in the
   *     cluster.
   */
  int getMaxGlobalWidth();

  /**
   * @return Factor by which a node with endpoint affinity will be favored while creating
   *     assignment.
   */
  double getAffinityFactor();

  /**
   * @return Whether to use the new assignment creator or the old one.
   */
  boolean useNewAssignmentCreator();

  /**
   * @return The assignment balance factor.
   */
  double getAssignmentCreatorBalanceFactor();

  /**
   * @return true if affinity should be ignored for leaf fragments.
   */
  boolean shouldIgnoreLeafAffinity();
}
