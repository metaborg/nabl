package mb.nabl2.terms.build;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map.Entry;

import jakarta.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;

import io.usethesource.capsule.Map;
import mb.nabl2.terms.IAttachments;

public class Attachments implements IAttachments, Serializable {
    private static final long serialVersionUID = 1L;

    private static final IAttachments EMPTY = new EmptyAttachments();

    private final Map.Immutable<Class<?>, Object> attachments;

    private Attachments(Map.Immutable<Class<?>, Object> attachments) {
        this.attachments = attachments;
    }

    @SuppressWarnings("unchecked") @Override @Nullable public <T> T get(Class<T> key) {
        return (T) attachments.get(key);
    }

    @Override public boolean isEmpty() {
        return attachments.isEmpty();
    }

    @Override public Builder toBuilder() {
        return new Builder(attachments.asTransient());
    }

    @Override public int hashCode() {
        return attachments.hashCode();
    }

    @Override public boolean equals(Object obj) {
        if(obj == null)
            return false;
        if(obj == this)
            return true;
        if(obj.getClass() != getClass())
            return false;
        Attachments other = (Attachments) obj;
        return other.attachments.equals(attachments);
    }

    public static IAttachments empty() {
        return EMPTY;
    }

    public static <T> Attachments of(Class<T> cls, T value) {
        return new Attachments(Map.Immutable.of(cls, value));
    }

    public static <T1, T2> Attachments of(Class<T1> cls1, T1 value1, Class<T2> cls2, T2 value2) {
        return new Attachments(Map.Immutable.of(cls1, value1, cls2, value2));
    }

    private static class EmptyAttachments implements IAttachments, Serializable {
        private static final long serialVersionUID = 1L;

        @Override public boolean isEmpty() {
            return true;
        }

        @Override public <T> T get(@SuppressWarnings("unused") Class<T> cls) {
            return null;
        }

        @Override public Builder toBuilder() {
            return new Attachments.Builder(null);
        }

    }

    private static class SingleAttachment implements IAttachments, Serializable {
        private static final long serialVersionUID = 1L;

        private final Class<?> cls;
        private final Object value;

        public SingleAttachment(Class<?> cls, Object value) {
            this.cls = cls;
            this.value = value;
        }

        @Override public boolean isEmpty() {
            return false;
        }

        @SuppressWarnings("unchecked") @Override public <T> T get(Class<T> cls) {
            if(this.cls.equals(cls)) {
                return (T) value;
            } else {
                return null;
            }
        }

        @Override public Builder toBuilder() {
            return new Attachments.Builder(Map.Transient.of(cls, value));
        }

    }

    private static class TwoAttachments implements IAttachments, Serializable {
        private static final long serialVersionUID = 1L;

        private final Class<?> cls1;
        private final Object value1;

        private final Class<?> cls2;
        private final Object value2;

        public TwoAttachments(Class<?> cls1, Object value1, Class<?> cls2, Object value2) {
            this.cls1 = cls1;
            this.value1 = value1;
            this.cls2 = cls2;
            this.value2 = value2;
        }

        @Override public boolean isEmpty() {
            return false;
        }

        @SuppressWarnings("unchecked") @Override public <T> T get(Class<T> cls) {
            if(this.cls1.equals(cls)) {
                return (T) value1;
            } else if(this.cls2.equals(cls)) {
                return (T) value2;
            } else {
                return null;
            }
        }

        @Override public Builder toBuilder() {
            return new Attachments.Builder(Map.Transient.of(cls1, value1, cls2, value2));
        }

    }

    private static class ThreeAttachments implements IAttachments, Serializable {
        private static final long serialVersionUID = 1L;

        private final Class<?> cls1;
        private final Object value1;

        private final Class<?> cls2;
        private final Object value2;

        private final Class<?> cls3;
        private final Object value3;

        public ThreeAttachments(Class<?> cls1, Object value1, Class<?> cls2, Object value2, Class<?> cls3,
                Object value3) {
            this.cls1 = cls1;
            this.value1 = value1;
            this.cls2 = cls2;
            this.value2 = value2;
            this.cls3 = cls3;
            this.value3 = value3;
        }

        @Override public boolean isEmpty() {
            return false;
        }

        @SuppressWarnings("unchecked") @Override public <T> T get(Class<T> cls) {
            if(this.cls1.equals(cls)) {
                return (T) value1;
            } else if(this.cls2.equals(cls)) {
                return (T) value2;
            } else if(this.cls3.equals(cls)) {
                return (T) value3;
            } else {
                return null;
            }
        }

        @Override public Builder toBuilder() {
            return new Attachments.Builder(Map.Transient.of(cls1, value1, cls2, value2, cls3, value3));
        }

    }

    private static class FourAttachments implements IAttachments, Serializable {
        private static final long serialVersionUID = 1L;

        private final Class<?> cls1;
        private final Object value1;

        private final Class<?> cls2;
        private final Object value2;

        private final Class<?> cls3;
        private final Object value3;

        private final Class<?> cls4;
        private final Object value4;

        public FourAttachments(Class<?> cls1, Object value1, Class<?> cls2, Object value2, Class<?> cls3, Object value3,
                Class<?> cls4, Object value4) {
            this.cls1 = cls1;
            this.value1 = value1;
            this.cls2 = cls2;
            this.value2 = value2;
            this.cls3 = cls3;
            this.value3 = value3;
            this.cls4 = cls4;
            this.value4 = value4;
        }

        @Override public boolean isEmpty() {
            return false;
        }

        @SuppressWarnings("unchecked") @Override public <T> T get(Class<T> cls) {
            if(this.cls1.equals(cls)) {
                return (T) value1;
            } else if(this.cls2.equals(cls)) {
                return (T) value2;
            } else if(this.cls3.equals(cls)) {
                return (T) value3;
            } else if(this.cls4.equals(cls)) {
                return (T) value4;
            } else {
                return null;
            }
        }

        @Override public Builder toBuilder() {
            return new Attachments.Builder(Map.Transient.of(cls1, value1, cls2, value2, cls3, value3, cls4, value4));
        }

    }

    public static class Builder implements IAttachments.Builder {

        private Map.Transient<Class<?>, Object> attachments;

        private Builder(Map.Transient<Class<?>, Object> attachments) {
            this.attachments = attachments;
        }

        @Override public <T> void put(Class<T> key, T value) {
            if(attachments == null) {
                attachments = CapsuleUtil.transientMap();
            }
            attachments.__put(key, value);
        }

        @Override public IAttachments build() {
            if(attachments == null) {
                return EMPTY;
            }
            final Iterator<Entry<Class<?>, Object>> it = attachments.entryIterator();
            switch(attachments.size()) {
                case 0:
                    return EMPTY;
                case 1: {
                    final Entry<Class<?>, Object> e1 = it.next();
                    return new SingleAttachment(e1.getKey(), e1.getValue());
                }
                case 2: {
                    final Entry<Class<?>, Object> e1 = it.next();
                    final Entry<Class<?>, Object> e2 = it.next();
                    return new TwoAttachments(e1.getKey(), e1.getValue(), e2.getKey(), e2.getValue());
                }
                case 3: {
                    final Entry<Class<?>, Object> e1 = it.next();
                    final Entry<Class<?>, Object> e2 = it.next();
                    final Entry<Class<?>, Object> e3 = it.next();
                    return new ThreeAttachments(e1.getKey(), e1.getValue(), e2.getKey(), e2.getValue(), e3.getKey(),
                            e3.getValue());
                }
                case 4: {
                    final Entry<Class<?>, Object> e1 = it.next();
                    final Entry<Class<?>, Object> e2 = it.next();
                    final Entry<Class<?>, Object> e3 = it.next();
                    final Entry<Class<?>, Object> e4 = it.next();
                    return new FourAttachments(e1.getKey(), e1.getValue(), e2.getKey(), e2.getValue(), e3.getKey(),
                            e3.getValue(), e4.getKey(), e4.getValue());
                }
                default:
                    return new Attachments(attachments.freeze());
            }
        }

        public static Builder of() {
            return new Builder(null);
        }

    }

}
