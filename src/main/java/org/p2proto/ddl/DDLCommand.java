package org.p2proto.ddl;

import liquibase.change.Change;

public interface DDLCommand {
    Change getChange();
}
