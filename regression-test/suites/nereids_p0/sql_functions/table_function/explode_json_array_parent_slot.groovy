// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

suite("explode_json_array_parent_slot") {
    sql "SET enable_nereids_planner=true"
    sql "SET enable_fallback_to_original_planner=false"

    sql """ DROP TABLE IF EXISTS explode_json_array_parent_slot """
    sql """
        CREATE TABLE IF NOT EXISTS explode_json_array_parent_slot (
            import_time DATETIME NULL,
            data TEXT NULL
        ) ENGINE=OLAP
        DUPLICATE KEY(import_time)
        DISTRIBUTED BY HASH(import_time) BUCKETS 1
        PROPERTIES ("replication_allocation" = "tag.location.default: 1");
    """

    sql """
        INSERT INTO explode_json_array_parent_slot VALUES
        ('2026-03-30 08:17:03', '{"data":{"data":{"pages.ranking.products":[{"id":"p1"},{"id":"p2"}]}}}'),
        ('2026-03-30 08:17:04', '{"data":{"data":{"pages.ranking.products":[{"id":"p3"},{"id":"p4"}]}}}'),
        ('2026-04-06 08:16:44', '{"data":{"data":{"pages.ranking.products":[{"id":"p5"},{"id":"p6"}]}}}'),
        ('2026-04-06 08:16:45', '{"data":{"data":{"pages.ranking.products":[{"id":"p7"},{"id":"p8"}]}}}');
    """

    qt_base_rows """
        SELECT import_time
        FROM explode_json_array_parent_slot
        ORDER BY import_time DESC;
    """

    qt_explode_order_by_parent_slot """
        SELECT import_time
        FROM (
            SELECT
                import_time,
                CAST(jsonb_extract_string(data, '$.data.data.pages\\.ranking\\.products') AS JSON) AS arr
            FROM explode_json_array_parent_slot
        ) s
        LATERAL VIEW explode_json_array_json(CAST(arr AS TEXT)) tmp AS elem
        ORDER BY import_time DESC
        LIMIT 6;
    """

    qt_explode_max_parent_slot """
        SELECT MAX(import_time)
        FROM (
            SELECT
                import_time,
                CAST(jsonb_extract_string(data, '$.data.data.pages\\.ranking\\.products') AS JSON) AS arr
            FROM explode_json_array_parent_slot
        ) s
        LATERAL VIEW explode_json_array_json(CAST(arr AS TEXT)) tmp AS elem;
    """

    qt_explode_distinct_parent_slot """
        SELECT DISTINCT import_time
        FROM (
            SELECT
                import_time,
                CAST(jsonb_extract_string(data, '$.data.data.pages\\.ranking\\.products') AS JSON) AS arr
            FROM explode_json_array_parent_slot
        ) s
        LATERAL VIEW explode_json_array_json(CAST(arr AS TEXT)) tmp AS elem
        ORDER BY import_time DESC;
    """
}
