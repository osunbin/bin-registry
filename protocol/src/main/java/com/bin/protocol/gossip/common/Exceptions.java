package com.bin.protocol.gossip.common;

public class Exceptions {

    public static RuntimeException propagate(Throwable t) {
        throwIfFatal(t);
        return (RuntimeException)(t instanceof RuntimeException ? (RuntimeException)t : new Exceptions.ReactiveException(t));
    }

    public static void throwIfFatal( Throwable t) {
        if (t != null) {
            if (isFatalButNotJvmFatal(t)) {
                throw (RuntimeException)t;
            } else if (isJvmFatal(t)) {
                throw (Error)t;
            }
        }
    }

    static boolean isFatalButNotJvmFatal( Throwable t) {
        return t instanceof Exceptions.BubblingException || t instanceof Exceptions.ErrorCallbackNotImplemented;
    }

    public static boolean isJvmFatal( Throwable t) {
        return t instanceof VirtualMachineError || t instanceof ThreadDeath || t instanceof LinkageError;
    }


    static class ReactiveException extends RuntimeException {
        private static final long serialVersionUID = 2491425227432776143L;

        ReactiveException(Throwable cause) {
            super(cause);
        }

        ReactiveException(String message) {
            super(message);
        }

        public synchronized Throwable fillInStackTrace() {
            return this.getCause() != null ? this.getCause().fillInStackTrace() : super.fillInStackTrace();
        }
    }

    static class BubblingException extends Exceptions.ReactiveException {
        private static final long serialVersionUID = 2491425277432776142L;

        BubblingException(String message) {
            super(message);
        }

        BubblingException(Throwable cause) {
            super(cause);
        }
    }

    static final class ErrorCallbackNotImplemented extends UnsupportedOperationException {
        private static final long serialVersionUID = 2491425227432776143L;

        ErrorCallbackNotImplemented(Throwable cause) {
            super(cause);
        }

        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

}
