package com.secretapp.backend.protocol

import com.secretapp.backend.data._
import com.secretapp.backend.protocol.{codecs => proto}

// to extend Codecs
trait Codecs extends proto.SexCodec
    with proto.StringCodec with proto.PackageCodec
    with proto.ProtoMessageCodec with proto.BytesCodec with proto.LongsCodec
    with proto.VarIntCodec with proto.RequestAuthIdCodec with proto.ResponseAuthIdCodec
    with proto.PingCodec with proto.PongCodec with proto.NewSessionCodec
    with proto.DropCodec with proto.SendSMSCodeCodec with proto.SentSMSCodeCodec
    with proto.SignUpCodec with proto.SignInCodec with proto.AuthorizationCodec
    with proto.RpcRequestCodec with proto.RpcResponseCodec

// to import Codecs._
object Codecs extends Codecs
