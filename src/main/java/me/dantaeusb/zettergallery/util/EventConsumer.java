package me.dantaeusb.zettergallery.util;

@FunctionalInterface
public interface EventConsumer {
    void accept();

    default EventConsumer andThen(EventConsumer after){
        return () -> {
            this.accept();
            after.accept();
        };
    }

    default EventConsumer compose(EventConsumer before){
        return () -> {
            before.accept();
            this.accept();
        };
    }
}
