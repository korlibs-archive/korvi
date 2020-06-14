export SCRIPTPATH="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
export FFMPEG_FILE=ffmpeg-4.2.3
7za -y x $FFMPEG_FILE.7z
cd $FFMPEG_FILE
./configure \
    --prefix=$SCRIPTPATH/ffmpeg \
    --disable-x86asm \
    --disable-everything \
    --disable-filters \
    --disable-avfilter \
    --disable-avdevice \
    --disable-swscale \
    --disable-swresample \
    --disable-ffplay \
    --disable-ffprobe \
    --enable-decoder=h264,aac \
    --enable-demuxer=mov \
    --enable-parser=h264 \
    --enable-protocol=file \
    --pkg-config-flags="--static" \
    --extra-cflags="-I$SCRIPTPATH/$FFMPEG_FILE/include -static" \
    --extra-ldflags="-L$SCRIPTPATH/$FFMPEG_FILE/lib -static"
make
make install
cd ..
