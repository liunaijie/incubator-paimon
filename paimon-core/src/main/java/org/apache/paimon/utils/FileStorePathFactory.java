/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.paimon.utils;

import org.apache.paimon.CoreOptions;
import org.apache.paimon.annotation.VisibleForTesting;
import org.apache.paimon.data.BinaryRow;
import org.apache.paimon.fs.Path;
import org.apache.paimon.io.DataFilePathFactory;
import org.apache.paimon.options.ConfigOption;
import org.apache.paimon.types.RowType;

import javax.annotation.concurrent.ThreadSafe;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.apache.paimon.options.ConfigOptions.key;

/** Factory which produces {@link Path}s for manifest files. */
@ThreadSafe
public class FileStorePathFactory {

    public static final ConfigOption<String> PARTITION_DEFAULT_NAME =
            key("partition.default-name")
                    .stringType()
                    .defaultValue("__DEFAULT_PARTITION__")
                    .withDescription(
                            "The default partition name in case the dynamic partition"
                                    + " column value is null/empty string.");

    private final Path root;
    private final String uuid;
    private final RowDataPartitionComputer partitionComputer;
    private final String formatIdentifier;

    private final AtomicInteger manifestFileCount;
    private final AtomicInteger manifestListCount;
    private final AtomicInteger indexManifestCount;
    private final AtomicInteger indexFileCount;

    public FileStorePathFactory(Path root) {
        this(
                root,
                RowType.builder().build(),
                PARTITION_DEFAULT_NAME.defaultValue(),
                CoreOptions.FILE_FORMAT.defaultValue().toString());
    }

    // for tables without partition, partitionType should be a row type with 0 columns (not null)
    public FileStorePathFactory(
            Path root, RowType partitionType, String defaultPartValue, String formatIdentifier) {
        this.root = root;
        this.uuid = UUID.randomUUID().toString();

        this.partitionComputer = getPartitionComputer(partitionType, defaultPartValue);
        this.formatIdentifier = formatIdentifier;

        this.manifestFileCount = new AtomicInteger(0);
        this.manifestListCount = new AtomicInteger(0);
        this.indexManifestCount = new AtomicInteger(0);
        this.indexFileCount = new AtomicInteger(0);
    }

    public Path root() {
        return root;
    }

    @VisibleForTesting
    public static RowDataPartitionComputer getPartitionComputer(
            RowType partitionType, String defaultPartValue) {
        String[] partitionColumns = partitionType.getFieldNames().toArray(new String[0]);
        return new RowDataPartitionComputer(defaultPartValue, partitionType, partitionColumns);
    }

    public Path newManifestFile() {
        return new Path(
                root + "/manifest/manifest-" + uuid + "-" + manifestFileCount.getAndIncrement());
    }

    public Path newManifestList() {
        return new Path(
                root
                        + "/manifest/manifest-list-"
                        + uuid
                        + "-"
                        + manifestListCount.getAndIncrement());
    }

    public Path toManifestFilePath(String manifestFileName) {
        return new Path(root + "/manifest/" + manifestFileName);
    }

    public Path toManifestListPath(String manifestListName) {
        return new Path(root + "/manifest/" + manifestListName);
    }

    public DataFilePathFactory createDataFilePathFactory(BinaryRow partition, int bucket) {
        return new DataFilePathFactory(
                root, getPartitionString(partition), bucket, formatIdentifier);
    }

    public Path bucketPath(BinaryRow partition, int bucket) {
        return DataFilePathFactory.bucketPath(root, getPartitionString(partition), bucket);
    }

    /** IMPORTANT: This method is NOT THREAD SAFE. */
    public String getPartitionString(BinaryRow partition) {
        return PartitionPathUtils.generatePartitionPath(
                partitionComputer.generatePartValues(
                        Preconditions.checkNotNull(
                                partition, "Partition row data is null. This is unexpected.")));
    }

    public List<Path> getHierarchicalPartitionPath(BinaryRow partition) {
        return PartitionPathUtils.generateHierarchicalPartitionPaths(
                        partitionComputer.generatePartValues(
                                Preconditions.checkNotNull(
                                        partition,
                                        "Partition binary row is null. This is unexpected.")))
                .stream()
                .map(p -> new Path(root + "/" + p))
                .collect(Collectors.toList());
    }

    @VisibleForTesting
    public String uuid() {
        return uuid;
    }

    public PathFactory manifestFileFactory() {
        return new PathFactory() {
            @Override
            public Path newPath() {
                return newManifestFile();
            }

            @Override
            public Path toPath(String fileName) {
                return toManifestFilePath(fileName);
            }
        };
    }

    public PathFactory manifestListFactory() {
        return new PathFactory() {
            @Override
            public Path newPath() {
                return newManifestList();
            }

            @Override
            public Path toPath(String fileName) {
                return toManifestListPath(fileName);
            }
        };
    }

    public PathFactory indexManifestFileFactory() {
        return new PathFactory() {
            @Override
            public Path newPath() {
                return new Path(
                        root
                                + "/manifest/index-manifest-"
                                + uuid
                                + "-"
                                + indexManifestCount.getAndIncrement());
            }

            @Override
            public Path toPath(String fileName) {
                return new Path(root + "/manifest/" + fileName);
            }
        };
    }

    public PathFactory indexFileFactory() {
        return new PathFactory() {
            @Override
            public Path newPath() {
                return new Path(
                        root + "/index/index-" + uuid + "-" + indexFileCount.getAndIncrement());
            }

            @Override
            public Path toPath(String fileName) {
                return new Path(root + "/index/" + fileName);
            }
        };
    }
}
