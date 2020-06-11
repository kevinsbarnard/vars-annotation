package org.mbari.vars.services.vampiresquid.v1;



import static org.junit.Assert.*;
import org.junit.Ignore;

import org.junit.Test;
import org.mbari.vars.services.MediaService;
import org.mbari.vars.services.TestToolbox;
import org.mbari.vars.services.model.Media;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Brian Schlining
 * @since 2018-01-03T11:56:00
 */
public class MediaServiceTest {
    MediaService mediaService = TestToolbox.getServices().getMediaService();

    // THis passes but we're turning it off so that we don't put
    // bogus values in the database
//    @Ignore
    @Test
    public void testCreate() throws InterruptedException,
            ExecutionException,
            URISyntaxException,
            TimeoutException {
        Instant now = Instant.now();
        CompletableFuture<Media> f = mediaService.create("Test-01",
                "Test", "Test-01-" + now,
                new URI("urn:rtva:org.mbari:" + "Test-01"), now);
        Media media = f.get(5000, TimeUnit.MILLISECONDS);
        assertNotNull(media);
    }

    @Test
    public void testCreateMedia() throws Exception {
        Instant now = Instant.now();
        var media = new Media();
        media.setVideoSequenceName("Test-02");
        media.setCameraId("Test");
        media.setVideoName("Test-02-" + now);
        media.setUri(URI.create("http://www.foo.bar/test/Test-02-" + now.toEpochMilli() + ".mp4"));
        media.setStartTimestamp(now);
        var f = mediaService.create(media);
        var m = f.get(5000, TimeUnit.MILLISECONDS);
        assertNotNull(m);
        assertEquals(media.getVideoSequenceName(), m.getVideoSequenceName());
        assertEquals(media.getCameraId(), m.getCameraId());
        assertEquals(media.getVideoName(), m.getVideoName());
        assertEquals(media.getStartTimestamp(), m.getStartTimestamp());
        assertEquals(media.getUri(), m.getUri());
        assertNotNull(m.getVideoSequenceUuid());
        assertNotNull(m.getVideoUuid());
        assertNotNull(m.getVideoReferenceUuid());

    }


    @Test
    public void testFindByVideoSequenceName() throws Exception {
        CompletableFuture<List<Media>> f = mediaService.findByVideoSequenceName("Ventana 3937");
        List<Media> media = f.get(5000, TimeUnit.MILLISECONDS);
        assertTrue(!media.isEmpty());
    }

    @Test
    public void testFindByUri() throws Exception {
        var now = Instant.now();
        var name = "Test " + Instant.now();
        var uri = URI.create("http://m3.shore.mbari.org/videos/M3/test/" + now.toEpochMilli() + ".mp4");
        var createFuture = mediaService.create(name, "Test", name, uri, now);
        var m0 = createFuture.get(5000, TimeUnit.MILLISECONDS);
        assertNotNull(m0);

        var f = mediaService.findByUri(uri);
        var m1 = f.get(5000, TimeUnit.MILLISECONDS);
        assertNotNull(m1);
        assertEquals(m0.getUri(), m0.getUri());
        assertEquals(m0.getStartTimestamp().toEpochMilli(), m1.getStartTimestamp().toEpochMilli());
        assertEquals(m0.getVideoSequenceName(), m1.getVideoSequenceName());
        assertEquals(m0.getVideoName(), m1.getVideoName());
    }

    @Test
    public void testFindByMissingUri() throws Exception {
        var f = mediaService.findByUri(URI.create("urn:i.dont.exist:v1234"));
        var m = f.get(5000, TimeUnit.MILLISECONDS);
        assertNull(m);
    }


}
