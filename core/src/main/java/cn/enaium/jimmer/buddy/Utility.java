/*
 * Copyright 2025 Enaium
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

package cn.enaium.jimmer.buddy;

import org.babyfish.jimmer.Formula;
import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.error.ErrorFamily;
import org.babyfish.jimmer.error.ErrorField;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.LogicalDeletedValueGenerator;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;

/**
 * @author Enaium
 */
public class Utility {
    public static Immutable immutable() {
        return new Immutable() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Immutable.class;
            }
        };
    }

    public static Entity entity() {
        return new Entity() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Entity.class;
            }

            @Override
            public String microServiceName() {
                return "";
            }
        };
    }

    public static MappedSuperclass mappedSuperclass() {
        return new MappedSuperclass() {
            @Override
            public boolean acrossMicroServices() {
                return false;
            }

            @Override
            public String microServiceName() {
                return "";
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return MappedSuperclass.class;
            }
        };
    }

    public static Embeddable embeddable() {
        return new Embeddable() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Embeddable.class;
            }
        };
    }

    public static ErrorFamily errorFamily() {
        return new ErrorFamily() {
            @Override
            public String value() {
                return "";
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return ErrorFamily.class;
            }
        };
    }

    public static ErrorField errorField() {
        return new ErrorField() {
            @Override
            public String name() {
                return "";
            }

            @Override
            public Class<?> type() {
                return null;
            }

            @Override
            public boolean list() {
                return false;
            }

            @Override
            public boolean nullable() {
                return false;
            }

            @Override
            public String doc() {
                return "";
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return ErrorField.class;
            }
        };
    }

    public static Id id() {
        return new Id() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Id.class;
            }
        };
    }

    public static IdView idView() {
        return new IdView() {
            @Override
            public String value() {
                return "";
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return IdView.class;
            }
        };
    }

    public static Key key() {
        return new Key() {
            @Override
            public String group() {
                return "";
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Key.class;
            }
        };
    }

    public static Version version() {
        return new Version() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Version.class;
            }
        };
    }

    public static Formula formula() {
        return new Formula() {
            @Override
            public String sql() {
                return "";
            }

            @Override
            public String[] dependencies() {
                return new String[0];
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Formula.class;
            }
        };
    }

    public static OneToOne oneToOne() {
        return new OneToOne() {
            @Override
            public String mappedBy() {
                return "";
            }

            @Override
            public boolean inputNotNull() {
                return false;
            }

            @Override
            public TargetTransferMode targetTransferMode() {
                return null;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return OneToOne.class;
            }
        };
    }

    public static OneToMany oneToMany() {
        return new OneToMany() {
            @Override
            public String mappedBy() {
                return "";
            }

            @Override
            public OrderedProp[] orderedProps() {
                return new OrderedProp[0];
            }

            @Override
            public TargetTransferMode targetTransferMode() {
                return null;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return OneToMany.class;
            }
        };
    }

    public static ManyToOne manyToOne() {
        return new ManyToOne() {
            @Override
            public boolean inputNotNull() {
                return false;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return ManyToOne.class;
            }
        };
    }

    public static ManyToMany manyToMany() {
        return new ManyToMany() {
            @Override
            public String mappedBy() {
                return "";
            }

            @Override
            public OrderedProp[] orderedProps() {
                return new OrderedProp[0];
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return ManyToMany.class;
            }
        };
    }

    public static ManyToManyView manyToManyView() {
        return new ManyToManyView() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return ManyToManyView.class;
            }

            @Override
            public String prop() {
                return "";
            }

            @Override
            public String deeperProp() {
                return "";
            }
        };
    }

    public static Column column() {
        return new Column() {
            @Override
            public String name() {
                return "";
            }

            @Override
            public String sqlType() {
                return "";
            }

            @Override
            public String sqlElementType() {
                return "";
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Column.class;
            }
        };
    }

    public static GeneratedValue generatedValue() {
        return new GeneratedValue() {
            @Override
            public GenerationType strategy() {
                return GenerationType.AUTO;
            }

            @Override
            public Class<? extends UserIdGenerator<?>> generatorType() {
                return null;
            }

            @Override
            public String generatorRef() {
                return "";
            }

            @Override
            public String sequenceName() {
                return "";
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return GeneratedValue.class;
            }
        };
    }

    public static JoinColumn joinColumn() {
        return new JoinColumn() {
            @Override
            public String name() {
                return "";
            }

            @Override
            public String referencedColumnName() {
                return "";
            }

            @Override
            public ForeignKeyType foreignKeyType() {
                return null;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return JoinColumn.class;
            }
        };
    }

    public static JoinTable joinTable() {
        return new JoinTable() {
            @Override
            public String name() {
                return "";
            }

            @Override
            public String schema() {
                return "";
            }

            @Override
            public String joinColumnName() {
                return "";
            }

            @Override
            public String inverseJoinColumnName() {
                return "";
            }

            @Override
            public JoinColumn[] joinColumns() {
                return new JoinColumn[0];
            }

            @Override
            public JoinColumn[] inverseJoinColumns() {
                return new JoinColumn[0];
            }

            @Override
            public boolean readonly() {
                return false;
            }

            @Override
            public boolean preventDeletionBySource() {
                return false;
            }

            @Override
            public boolean preventDeletionByTarget() {
                return false;
            }

            @Override
            public boolean cascadeDeletedBySource() {
                return false;
            }

            @Override
            public boolean cascadeDeletedByTarget() {
                return false;
            }

            @Override
            public boolean deletedWhenEndpointIsLogicallyDeleted() {
                return false;
            }

            @Override
            public JoinTableFilter filter() {
                return null;
            }

            @Override
            public LogicalDeletedFilter logicalDeletedFilter() {
                return null;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return JoinTable.class;
            }
        };
    }

    public static Transient _transient() {
        return new Transient() {
            @Override
            public Class<?> value() {
                return null;
            }

            @Override
            public String ref() {
                return "";
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Transient.class;
            }
        };
    }

    public static Serialized serialized() {
        return new Serialized() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Serialized.class;
            }
        };
    }

    public static LogicalDeleted logicalDeleted() {
        return new LogicalDeleted() {
            @Override
            public String value() {
                return "";
            }

            @Override
            public Class<? extends LogicalDeletedValueGenerator<?>> generatorType() {
                return null;
            }

            @Override
            public String generatorRef() {
                return "";
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return LogicalDeleted.class;
            }
        };
    }

    public static Nullable nullable() {
        return new Nullable() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Nullable.class;
            }

            @Override
            public String value() {
                return "";
            }
        };
    }
}
