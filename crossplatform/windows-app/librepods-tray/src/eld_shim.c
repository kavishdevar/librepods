// Minimal AAC-ELD decoder shim over FFmpeg libavcodec (LGPL). Exposes a small,
// version-stable C ABI so the Rust side never touches AVCodecContext internals.
#include <libavcodec/avcodec.h>
#include <libavutil/frame.h>
#include <libavutil/mem.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

typedef struct {
    AVCodecContext *ctx;
    AVPacket *pkt;
    AVFrame  *frame;
} EldDec;

// Open an AAC-ELD decoder configured by `asc` (AudioSpecificConfig). Returns an
// opaque handle or NULL.
void *eld_open(const uint8_t *asc, int asc_len, int sample_rate) {
    const AVCodec *c = avcodec_find_decoder_by_name("aac");
    if (!c) return NULL;
    AVCodecContext *ctx = avcodec_alloc_context3(c);
    if (!ctx) return NULL;
    ctx->extradata = (uint8_t *)av_malloc(asc_len + AV_INPUT_BUFFER_PADDING_SIZE);
    if (!ctx->extradata) { avcodec_free_context(&ctx); return NULL; }
    memset(ctx->extradata, 0, asc_len + AV_INPUT_BUFFER_PADDING_SIZE);
    memcpy(ctx->extradata, asc, asc_len);
    ctx->extradata_size = asc_len;
    ctx->sample_rate = sample_rate;              // channels/rate also come from the ASC
    if (avcodec_open2(ctx, c, NULL) < 0) { avcodec_free_context(&ctx); return NULL; }
    EldDec *d = (EldDec *)malloc(sizeof(EldDec));
    if (!d) { avcodec_free_context(&ctx); return NULL; }
    d->ctx = ctx;
    d->pkt = av_packet_alloc();
    d->frame = av_frame_alloc();
    return d;
}

// Decode one access unit into interleaved mono i16 `out` (capacity `out_cap`
// samples). Returns the number of samples written, or -1 on error.
int eld_decode(void *handle, const uint8_t *au, int au_len, int16_t *out, int out_cap) {
    EldDec *d = (EldDec *)handle;
    if (!d) return -1;
    d->pkt->data = (uint8_t *)au;
    d->pkt->size = au_len;
    if (avcodec_send_packet(d->ctx, d->pkt) < 0) return -1;
    int total = 0;
    while (avcodec_receive_frame(d->ctx, d->frame) == 0) {
        int n = d->frame->nb_samples;
        const float *p = (const float *)d->frame->data[0];  // FLTP mono -> plane 0
        for (int i = 0; i < n && total < out_cap; i++) {
            float s = p[i];
            if (s > 1.0f) s = 1.0f;
            if (s < -1.0f) s = -1.0f;
            out[total++] = (int16_t)(s * 32767.0f);
        }
        av_frame_unref(d->frame);
    }
    return total;
}

void eld_close(void *handle) {
    EldDec *d = (EldDec *)handle;
    if (!d) return;
    av_frame_free(&d->frame);
    av_packet_free(&d->pkt);
    avcodec_free_context(&d->ctx);
    free(d);
}
