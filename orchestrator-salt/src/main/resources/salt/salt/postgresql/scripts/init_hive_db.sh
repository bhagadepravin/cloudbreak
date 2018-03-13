#!/usr/bin/env bash

set -ex

if [ ! -f "/var/ambari-init-executed" ]; then
    echo "Create Hive database"
    echo "CREATE DATABASE $DATABASE;" | psql -U postgres
    echo "CREATE USER $USER WITH PASSWORD '$PASSWORD';" | psql -U postgres
    echo "GRANT ALL PRIVILEGES ON DATABASE $USER TO $DATABASE;" | psql -U postgres

    echo "Add access to pg_hba.conf"
    DATADIR=$(psql -c "show data_directory;" | grep "/data")
    echo "Datadir: $DATADIR"
    echo "host $DATABASE $USER 0.0.0.0/0 md5" >> $DATADIR/pg_hba.conf
    echo "local $DATABASE $USER md5" >> $DATADIR/pg_hba.conf
    echo $(date +%Y-%m-%d:%H:%M:%S) >> $DATADIR/init_hive_db_executed

fi
