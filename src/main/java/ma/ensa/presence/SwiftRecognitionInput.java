package ma.ensa.presence;

import java.net.URI;

final class SwiftRecognitionInput implements PhotoUploadConsumer.Handler {
    @FunctionalInterface
    interface Recognizer {
        void recognize(PhotoUploadedEvent event, byte[] photo) throws Exception;
    }

    private final SwiftPhotoStore store = new SwiftPhotoStore();
    private final URI container;
    private final String token;
    private final Recognizer recognizer;

    SwiftRecognitionInput(URI container, String token, Recognizer recognizer) {
        this.container = container;
        this.token = token;
        this.recognizer = recognizer;
    }

    @Override
    public void process(PhotoUploadedEvent event) throws Exception {
        byte[] photo = store.download(container, token, event.swiftObjectKey());
        recognizer.recognize(event, photo);
    }
}
