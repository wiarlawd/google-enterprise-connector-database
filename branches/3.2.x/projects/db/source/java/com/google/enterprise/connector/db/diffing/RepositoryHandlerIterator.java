// Copyright 2011 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.enterprise.connector.db.diffing;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.enterprise.connector.util.diffing.DocumentSnapshot;

import java.util.Iterator;

/**
 * Iterates over the collections of {@link DocumentSnapshot} objects
 * produced by a {@code RepositoryHandler}.
 */
public class RepositoryHandlerIterator
    extends AbstractIterator<DocumentSnapshot> {
  private final RepositoryHandler repositoryHandler;
  private Iterator<DocumentSnapshot> current;

  /**
   * @param repositoryHandler RepositoryHandler object for fetching DB rows in
   *        DocumentSnapshot form.
   */
  public RepositoryHandlerIterator(RepositoryHandler repositoryHandler) {
    this.repositoryHandler = repositoryHandler;
    this.current = Iterators.emptyIterator();
  }

  @Override
  protected DocumentSnapshot computeNext() {
    if (current.hasNext()) {
      return current.next();
    } else {
      current = repositoryHandler.executeQueryAndAddDocs().iterator();
      if (current.hasNext()) {
        return current.next();
      } else {
        return endOfData();
      }
    }
  }
}
