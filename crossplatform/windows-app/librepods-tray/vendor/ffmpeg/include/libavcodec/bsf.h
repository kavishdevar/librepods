/*
 * Bitstream filters public API
 *
 * This file is part of FFmpeg.
 *
 * FFmpeg is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * FFmpeg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with FFmpeg; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

#ifndef AVCODEC_BSF_H
#define AVCODEC_BSF_H

#include "libavutil/dict.h"
#include "libavutil/log.h"
#include "libavutil/rational.h"

#include "codec_id.h"
#include "codec_par.h"
#include "packet.h"

/**
 * @defgroup lavc_bsf Bitstream filters
 * @ingroup libavc
 *
 * Bitstream filters transform encoded media data without decoding it. This
 * allows e.g. manipulating various header values. Bitstream filters operate on
 * @ref AVPacket "AVPackets".
 *
 * The bitstream filtering API is centered around two structures:
 * AVBitStreamFilter and AVBSFContext. The former represents a bitstream filter
 * in abstract, the latter a specific filtering process. Obtain an
 * AVBitStreamFilter using av_bsf_get_by_name() or av_bsf_iterate(), then pass
 * it to av_bsf_alloc() to create an AVBSFContext. Fill in the user-settable
 * AVBSFContext fields, as described in its documentation, then call
 * av_bsf_init() to prepare the filter context for use.
 *
 * Submit packets for filtering using av_bsf_send_packet(), obtain filtered
 * results with av_bsf_receive_packet(). When no more input packets will be
 * sent, submit a NULL AVPacket to signal the end of the stream to the filter.
 * av_bsf_receive_packet() will then return trailing packets, if any are
 * produced by the filter.
 *
 * Finally, free the filter context with av_bsf_free().
 * @{
 */

/**
 * The bitstream filter state.
 *
 * This struct must be allocated with av_bsf_alloc() and freed with
 * av_bsf_free().
 *
 * The fields in the struct will only be changed (by the caller or by the
 * filter) as described in their documentation, and are to be considered
 * immutable otherwise.
 */
typedef struct AVBSFContext {
    /**
     * A class for logging and AVOptions
     */
    const AVClass *av_class;

    /**
     * The bitstream filter this context is an instance of.
     */
    const struct AVBitStreamFilter *filter;

    /**
     * Opaque filter-specific private data. If filter->priv_class is non-NULL,
     * this is an AVOptions-enabled struct.
     */
    void *priv_data;

    /**
     * Parameters of the input stream. This field is allocated in
     * av_bsf_alloc(), it needs to be filled by the caller before
     * av_bsf_init().
     */
    AVCodecParameters *par_in;

    /**
     * Parameters of the output stream. This field is allocated in
     * av_bsf_alloc(), it is set by the filter in av_bsf_init().
     */
    AVCodecParameters *par_out;

    /**
     * The timebase used for the timestamps of the input packets. Set by the
     * caller before av_bsf_init().
     */
    AVRational time_base_in;

    /**
     * The timebase used for the timestamps of the output packets. Set by the
     * filter in av_bsf_init().
     */
    AVRational time_base_out;
} AVBSFContext;

typedef struct AVBitStreamFilter {
    const char *name;

    /**
     * A list of codec ids supported by the filter, terminated by
     * AV_CODEC_ID_NONE.
     * May be NULL, in that case the bitstream filter works with any codec id.
     */
    const enum AVCodecID *codec_ids;

    /**
     * A class for the private data, used to declare bitstream filter private
     * AVOptions. This field is NULL for bitstream filters that do not declare
     * any options.
     *
     * If this field is non-NULL, the first member of the filter private data
     * must be a pointer to AVClass, which will be set by libavcodec generic
     * code to this class.
     */
    const AVClass *priv_class;
} AVBitStreamFilter;

/**
 * @return a bitstream filter with the specified name or NULL if no such
 *         bitstream filter exists.
 */
const AVBitStreamFilter *av_bsf_get_by_name(const char *name);

/**
 * Iterate over all registered bitstream filters.
 *
 * @param opaque a pointer where libavcodec will store the iteration state. Must
 *               point to NULL to start the iteration.
 *
 * @return the next registered bitstream filter or NULL when the iteration is
 *         finished
 */
const AVBitStreamFilter *av_bsf_iterate(void **opaque);

/**
 * Allocate a context for a given bitstream filter. The caller must fill in the
 * context parameters as described in the documentation and then call
 * av_bsf_init() before sending any data to the filter.
 *
 * @param filter the filter for which to allocate an instance.
 * @param[out] ctx a pointer into which the pointer to the newly-allocated context
 *                 will be written. It must be freed with av_bsf_free() after the
 *                 filtering is done.
 *
 * @return 0 on success, a negative AVERROR code on failure
 */
int av_bsf_alloc(const AVBitStreamFilter *filter, AVBSFContext **ctx);

/**
 * Prepare the filter for use, after all the parameters and options have been
 * set.
 *
 * @param ctx a AVBSFContext previously allocated with av_bsf_alloc()
 */
int av_bsf_init(AVBSFContext *ctx);

/**
 * Submit a packet for filtering.
 *
 * After sending each packet, the filter must be completely drained by calling
 * av_bsf_receive_packet() repeatedly until it returns AVERROR(EAGAIN) or
 * AVERROR_EOF.
 *
 * @param ctx an initialized AVBSFContext
 * @param pkt the packet to filter. The bitstream filter will take ownership of
 * the packet and reset the contents of pkt. pkt is not touched if an error occurs.
 * If pkt is empty (i.e. NULL, or pkt->data is NULL and pkt->side_data_elems zero),
 * it signals the end of the stream (i.e. no more non-empty packets will be sent;
 * sending more empty packets does nothing) and will cause the filter to output
 * any packets it may have buffered internally.
 *
 * @return
 *  - 0 on success.
 *  - AVERROR(EAGAIN) if packets need to be retrieved from the filter (using
 *    av_bsf_receive_packet()) before new input can be consumed.
 *  - Another negative AVERROR value if an error occurs.
 */
int av_bsf_send_packet(AVBSFContext *ctx, AVPacket *pkt);

/**
 * Retrieve a filtered packet.
 *
 * @param ctx an initialized AVBSFContext
 * @param[out] pkt this struct will be filled with the contents of the filtered
 *                 packet. It is owned by the caller and must be freed using
 *                 av_packet_unref() when it is no longer needed.
 *                 This parameter should be "clean" (i.e. freshly allocated
 *                 with av_packet_alloc() or unreffed with av_packet_unref())
 *                 when this function is called. If this function returns
 *                 successfully, the contents of pkt will be completely
 *                 overwritten by the returned data. On failure, pkt is not
 *                 touched.
 *
 * @return
 *  - 0 on success.
 *  - AVERROR(EAGAIN) if more packets need to be sent to the filter (using
 *    av_bsf_send_packet()) to get more output.
 *  - AVERROR_EOF if there will be no further output from the filter.
 *  - Another negative AVERROR value if an error occurs.
 *
 * @note one input packet may result in several output packets, so after sending
 * a packet with av_bsf_send_packet(), this function needs to be called
 * repeatedly until it stops returning 0. It is also possible for a filter to
 * output fewer packets than were sent to it, so this function may return
 * AVERROR(EAGAIN) immediately after a successful av_bsf_send_packet() call.
 */
int av_bsf_receive_packet(AVBSFContext *ctx, AVPacket *pkt);

/**
 * Reset the internal bitstream filter state. Should be called e.g. when seeking.
 */
void av_bsf_flush(AVBSFContext *ctx);

/**
 * Free a bitstream filter context and everything associated with it; write NULL
 * into the supplied pointer.
 */
void av_bsf_free(AVBSFContext **ctx);

/**
 * Get the AVClass for AVBSFContext. It can be used in combination with
 * AV_OPT_SEARCH_FAKE_OBJ for examining options.
 *
 * @see av_opt_find().
 */
const AVClass *av_bsf_get_class(void);

/**
 * Structure for chain/list of bitstream filters.
 * Empty list can be allocated by av_bsf_list_alloc().
 */
typedef struct AVBSFList AVBSFList;

/**
 * Allocate empty list of bitstream filters.
 * The list must be later freed by av_bsf_list_free()
 * or finalized by av_bsf_list_finalize().
 *
 * @return Pointer to @ref AVBSFList on success, NULL in case of failure
 */
AVBSFList *av_bsf_list_alloc(void);

/**
 * Free list of bitstream filters.
 *
 * @param lst Pointer to pointer returned by av_bsf_list_alloc()
 */
void av_bsf_list_free(AVBSFList **lst);

/**
 * Append bitstream filter to the list of bitstream filters.
 *
 * @param lst List to append to
 * @param bsf Filter context to be appended
 *
 * @return >=0 on success, negative AVERROR in case of failure
 */
int av_bsf_list_append(AVBSFList *lst, AVBSFContext *bsf);

/**
 * Construct new bitstream filter context given it's name and options
 * and append it to the list of bitstream filters.
 *
 * @param lst      List to append to
 * @param bsf_name Name of the bitstream filter
 * @param options  Options for the bitstream filter, can be set to NULL
 *
 * @return >=0 on success, negative AVERROR in case of failure
 */
int av_bsf_list_append2(AVBSFList *lst, const char * bsf_name, AVDictionary **options);
/**
 * Finalize list of bitstream filters.
 *
 * This function will transform @ref AVBSFList to single @ref AVBSFContext,
 * so the whole chain of bitstream filters can be treated as single filter
 * freshly allocated by av_bsf_alloc().
 * If the call is successful, @ref AVBSFList structure is freed and lst
 * will be set to NULL. In case of failure, caller is responsible for
 * freeing the structure by av_bsf_list_free()
 *
 * @param      lst Filter list structure to be transformed
 * @param[out] bsf Pointer to be set to newly created @ref AVBSFContext structure
 *                 representing the chain of bitstream filters
 *
 * @return >=0 on success, negative AVERROR in case of failure
 */
int av_bsf_list_finalize(AVBSFList **lst, AVBSFContext **bsf);

/**
 * Parse string describing list of bitstream filters and create single
 * @ref AVBSFContext describing the whole chain of bitstream filters.
 * Resulting @ref AVBSFContext can be treated as any other @ref AVBSFContext freshly
 * allocated by av_bsf_alloc().
 *
 * @param      str String describing chain of bitstream filters in format
 *                 `bsf1[=opt1=val1:opt2=val2][,bsf2]`
 * @param[out] bsf Pointer to be set to newly created @ref AVBSFContext structure
 *                 representing the chain of bitstream filters
 *
 * @return >=0 on success, negative AVERROR in case of failure
 */
int av_bsf_list_parse_str(const char *str, AVBSFContext **bsf);

/**
 * Get null/pass-through bitstream filter.
 *
 * @param[out] bsf Pointer to be set to new instance of pass-through bitstream filter
 *
 * @return
 */
int av_bsf_get_null_filter(AVBSFContext **bsf);

/**
 * @defgroup lavc_bsfgraph Bitstream filter graph
 * Experimental graph-based API for bitstream filters.
 * @{
 */

/**
 * A link between two filters. This contains pointers to the source and
 * destination filters between which this link exists, and the indexes of
 * the pads involved.
 */
typedef struct AVBitStreamFilterLink AVBitStreamFilterLink;

/**
 * A filter pad used for either input or output.
 */
typedef struct AVBitStreamFilterPad AVBitStreamFilterPad;

/** An instance of a filter */
typedef struct AVBitStreamFilterContext {
    /**
     * A class for logging and AVOptions
     */
    const AVClass *av_class;

    /**
     * The bitstream filter this context is an instance of.
     */
    const struct AVBitStreamFilter *filter;

    /**
     * name of this filter instance
     */
    char *name;

    AVBitStreamFilterPad  *input_pads; ///< array of input pads
    AVBitStreamFilterLink    **inputs; ///< array of pointers to input links
    unsigned                nb_inputs; ///< number of input pads

    AVBitStreamFilterPad *output_pads; ///< array of output pads
    AVBitStreamFilterLink   **outputs; ///< array of pointers to output links
    unsigned               nb_outputs; ///< number of output pads

    /**
     * Opaque filter-specific private data. If filter->priv_class is non-NULL,
     * this is an AVOptions-enabled struct.
     */
    void *priv_data;

    /**
     * filtergraph this filter belongs to
     */
    struct AVBitStreamFilterGraph *graph;
} AVBitStreamFilterContext;

/**
 * The number of the filter inputs is not determined just by the filter's static
 * inputs. The filter might add additional inputs during initialization depending
 * on the options supplied to it.
 */
#define AV_BSF_FLAG_DYNAMIC_INPUTS        (1 << 0)
/**
 * The number of the filter outputs is not determined just by the filter's static
 * outputs. The filter might add additional outputs during initialization depending
 * on the options supplied to it.
 */
#define AV_BSF_FLAG_DYNAMIC_OUTPUTS       (1 << 1)
/**
 * The filter is a "metadata" filter - it does not modify the packet data in any
 * way. It may only affect the metadata (i.e. those fields copied by
 * av_packet_copy_props()).
 *
 * More precisely, this means that the data of any packet output by the filter
 * must be exactly equal to some packet that is received on one of its inputs.
 * Furthermore, all packets produced on a given output must correspond to packet
 * received on the same input and their order must be unchanged.
 * Note that the filter may still drop or duplicate the frames.
 */
#define AV_BSF_FLAG_METADATA_ONLY         (1 << 2)

/**
 * Get the name of an AVBitStreamFilterPad.
 *
 * @param pads an array of AVBitStreamFilterPads
 * @param pad_idx index of the pad in the array; it is the caller's
 *                responsibility to ensure the index is valid
 *
 * @return name of the pad_idx'th pad in pads
 */
const char *av_bsf_pad_get_name(const AVBitStreamFilterPad *pads, int pad_idx);

/**
 * Get the codec ids supported by an AVBitStreamFilterPad.
 *
 * @param pads an array of AVBitStreamFilterPads
 * @param pad_idx index of the pad in the array; it is the caller's
 *                responsibility to ensure the index is valid
 *
 * @return an array of AVCodecID terminated by AV_CODEC_ID_NONE, or NULL
 *         if the pad has no codec id constrains.
 */
const enum AVCodecID *av_bsf_pad_get_codec_ids(const AVBitStreamFilterPad *pads, int pad_idx);

/**
 * Link two filters together.
 *
 * @param src    the source filter
 * @param srcpad index of the output pad on the source filter
 * @param dst    the destination filter
 * @param dstpad index of the input pad on the destination filter
 * @return       zero on success
 */
int av_bsf_link(AVBitStreamFilterContext *src, unsigned srcpad,
                AVBitStreamFilterContext *dst, unsigned dstpad);

/**
 * Initialize a filter with the supplied parameters.
 *
 * @param ctx  uninitialized filter context to initialize
 * @param args Options to initialize the filter with. This must be a
 *             ':'-separated list of options in the 'key=value' form.
 *             May be NULL if the options have been set directly using the
 *             AVOptions API or there are no options that need to be set.
 * @return 0 on success, a negative AVERROR on failure
 */
int av_bsf_init_str(AVBitStreamFilterContext *ctx, const char *args);

/**
 * Initialize a filter with the supplied dictionary of options.
 *
 * @param ctx     uninitialized filter context to initialize
 * @param options An AVDictionary filled with options for this filter. On
 *                return this parameter will be destroyed and replaced with
 *                a dict containing options that were not found. This dictionary
 *                must be freed by the caller.
 *                May be NULL, then this function is equivalent to
 *                av_bsf_init_str() with the second parameter set to NULL.
 * @return 0 on success, a negative AVERROR on failure
 *
 * @note This function and av_bsf_init_str() do essentially the same thing,
 * the difference is in manner in which the options are passed. It is up to the
 * calling code to choose whichever is more preferable. The two functions also
 * behave differently when some of the provided options are not declared as
 * supported by the filter. In such a case, av_bsf_init_str() will fail, but
 * this function will leave those extra options in the options AVDictionary and
 * continue as usual.
 */
int av_bsf_init_dict(AVBitStreamFilterContext *ctx, AVDictionary **options);

typedef struct AVBitStreamFilterGraph {
    const AVClass *av_class;

    AVBitStreamFilterContext **filters;

    unsigned nb_filters;

    /**
     * Sets the maximum number of buffered packets in the filtergraph combined.
     *
     * Zero means no limit. This field must be set before calling
     * av_bsf_graph_config().
     */
    unsigned max_buffered_packets;
} AVBitStreamFilterGraph;

/**
 * Allocate a filter graph.
 *
 * @return the allocated filter graph on success or NULL.
 */
AVBitStreamFilterGraph *av_bsf_graph_alloc(void);

/**
 * Create a new filter instance in a filter graph.
 *
 * @param[out] filt_ctx A pointer into which the pointer to the newly-allocated context
 *                      will be written on success. May be NULL. Note that it is also
 *                      retrievable directly through AVBitStreamFilterGraph.filters or
 *                      with @ref av_bsf_graph_get_filter().
 * @param[in] filter the filter to create an instance of
 * @param[in] name Name to give to the new instance (will be copied to
 *                 AVBitStreamFilterContext.name). This may be used by the caller to
 *                 identify different filters, libavcodec itself assigns no semantics
 *                 to this parameter. May be NULL.
 * @param[in] graph graph in which the new filter will be used
 *
 * @note On failure and if filt_ctx is not NULL, *filt_ctx will be set to NULL.
 * @return a negative AVERROR error code in case of failure, a non negative value otherwise
 */
int av_bsf_graph_alloc_filter(AVBitStreamFilterContext **filt_ctx,
                              const AVBitStreamFilter *filter,
                              const char *name,
                              AVBitStreamFilterGraph *graph);

/**
 * A convenience wrapper that allocates and initializes a filter in a single
 * step. The filter instance is created from the filter filt and inited with the
 * parameter args.
 *
 * @param[out] filt_ctx A pointer into which the pointer to the newly-allocated context
 *                      will be written on success. May be NULL. Note that it is also
 *                      retrievable directly through AVBitStreamFilterGraph.filters or
 *                      with @ref av_bsf_graph_get_filter().
 * @param[in] name the instance name to give to the created filter instance
 * @param[in] graph_ctx the filter graph
 * @return a negative AVERROR error code in case of failure, a non negative value otherwise
 *
 * @note On failure and if filt_ctx is not NULL, *filt_ctx will be set to NULL.
 * @warning Since the filter is initialized after this function successfully
 *          returns, you MUST NOT set any further options on it. If you need to
 *          do that, call ::av_bsf_graph_alloc_filter(), followed by setting
 *          the options, followed by ::av_bsf_init_dict() instead of this
 *          function.
 */
int av_bsf_graph_create_filter(AVBitStreamFilterContext **filt_ctx,
                               const AVBitStreamFilter *filt,
                               const char *name, AVDictionary **options,
                               AVBitStreamFilterGraph *graph_ctx);

/**
 * Get a filter instance identified by instance name from graph.
 *
 * @param graph filter graph to search through.
 * @param name filter instance name (should be unique in the graph).
 * @return the pointer to the found filter instance or NULL if it
 * cannot be found.
 */
AVBitStreamFilterContext *av_bsf_graph_get_filter(AVBitStreamFilterGraph *graph, const char *name);

/**
 * Check validity and configure all the links and formats in the graph.
 *
 * @param graphctx the filter graph
 * @param log_ctx context used for logging
 * @return >= 0 in case of success, a negative AVERROR code otherwise
 */
int av_bsf_graph_config(AVBitStreamFilterGraph *graphctx, void *log_ctx);

/**
 * Get the index of the source filter in the filtergraph that reported needing
 * input more urgently.
 *
 * @return the index value of a source filter in the filtergraph, or AVERROR(EOF)
 *         if no source is accepting more packets.
 */
int av_bsf_graph_source_needs_input(const AVBitStreamFilterGraph *graph);

/**
 * Free a graph, destroy its links, and set *graph to NULL.
 * If *graph is NULL, do nothing.
 */
void av_bsf_graph_free(AVBitStreamFilterGraph **graph);

/**
 * @defgroup lavc_bsfgraph_source Packet source API
 *
 * The source filter is there to connect filter graphs to applications
 * They have a single output, connected to the graph, and no input.
 * Packets must be fed to it using av_bsf_source_add_packet().
 * @{
 */

enum {
    /**
     * Immediately push the packet to the output.
     */
    AV_BSF_SOURCE_FLAG_PUSH = 1 << 0,

    /**
     * Keep a reference to the packet.
     */
    AV_BSF_SOURCE_FLAG_KEEP_REF = 1 << 1,
};

/**
 * Initialize the source filter with the provided parameters.
 * This function may be called multiple times, the later calls override the
 * previous ones. Some of the parameters may also be set through AVOptions, then
 * whatever method is used last takes precedence.
 *
 * @param ctx an instance of the source filter
 * @param param the stream parameters. The packet later passed to this filter
 *              must conform to those parameters. All the allocated fields in
 *              param remain owned by the caller, libavcodec will make internal
 *              copies or references when necessary.
 * @return 0 on success, a negative AVERROR code on failure.
 */
int av_bsf_source_parameters_set(AVBitStreamFilterContext *ctx, const AVCodecParameters *par);

/**
 * Add a packet to the buffer source.
 *
 * By default, this function will take ownership of the reference(s) and reset
 * the packet. This can be controlled using the flags.
 *
 * If this function returns an error, the input packet is not touched.
 *
 * @param buffer_src  pointer to a source filter context
 * @param packet      a packet, or NULL to mark EOF
 * @param flags       a combination of AV_BSF_FLAG_*
 * @return            >= 0 in case of success, a negative AVERROR code
 *                    in case of failure
 */
av_warn_unused_result
int av_bsf_source_add_packet(AVBitStreamFilterContext *ctx, AVPacket *pkt, int flags);

/**
 * Returns 0 or a negative AVERROR code. Currently, this will only ever
 * return AVERROR(EOF), to indicate that the buffer source has been closed,
 * either as a result of av_bsf_source_close(), or because the downstream
 * filter is no longer accepting new data.
 */
int av_bsf_source_get_status(AVBitStreamFilterContext *ctx);

/**
 * Close the source after EOF.
 *
 * This is similar to passing NULL to av_bsf_source_add_packet()
 * except it takes the timestamp of the EOF, i.e. the timestamp of the end
 * of the last packet.
 */
int av_bsf_source_close(AVBitStreamFilterContext *ctx, int64_t pts, unsigned flags);

/**
 * @}
 */

/**
 * @defgroup lavc_bsfgraph_sink Packet sink API
 * @{
 *
 * The sink filter is there to connect filter graphs to applications
 * They have a single input, connected to the graph, and no output.
 * Packets must be extracted using av_bsf_sink_get_packet().
 */

enum {
    /**
     * Tell av_buffersink_get_buffer_ref() to read video/samples buffer
     * reference, but not remove it from the buffer. This is useful if you
     * need only to read a video/samples buffer, without to fetch it.
     */
    AV_BSF_SINK_FLAG_PEEK = 1 << 0,

    /**
     * Tell av_bsf_sink_get_packet() not to request a packet from its input.
     * If a packet is already buffered, it is read (and removed from the buffer),
     * but if no packet is present, return AVERROR(EAGAIN).
     */
    AV_BSF_SINK_FLAG_NO_REQUEST = 1 << 1,
};

/**
 * Get a packet with filtered data from sink and put it in packet.
 *
 * @param ctx    pointer to a sink filter context.
 * @param packet pointer to an allocated packet that will be filled with data.
 *               The data must be freed using av_packet_unref() / av_packet_free()
 * @param flags  a combination of AV_BSF_SINK_FLAG_* flags
 *
 * @retval AVERROR(EAGAIN) output could not be produced.
 *                         if AV_BSF_SINK_FLAG_NO_REQUEST was not set,
 *                         @ref av_bsf_graph_needs_input can be called to
 *                         know which source needs input more urgently.
 * @retval >= 0            success
 * @retval "another negative error code" legitimate error
 */
int av_bsf_sink_get_packet(AVBitStreamFilterContext *ctx, AVPacket *pkt, int flags);

AVRational av_bsf_sink_get_time_base(const AVBitStreamFilterContext *ctx);
const AVCodecParameters *av_bsf_sink_get_parameters(const AVBitStreamFilterContext *ctx);

/**
 * @}
 *
 * @}
 *
 * @}
 */

#endif // AVCODEC_BSF_H
