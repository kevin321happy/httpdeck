import 'dart:async';
import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

/// Flutter/Dio adapter for HttpDeck.
///
/// The Android host owns the inspector panel. This package only forwards
/// Flutter Dio events to:
///
/// `io.github.kevin321happy.httpdeck/network`
///
/// with method `debugNetworkEvent`.
class HttpDeckFlutter {
  static const MethodChannel _channel =
      MethodChannel('io.github.kevin321happy.httpdeck/network');

  static const int _maxBodyChars = 64 * 1024;

  /// Debug builds capture by default; release builds do not.
  static bool captureEnabled = kDebugMode;

  /// Adds the Dio interceptor. Call before the first business request.
  static Future<void> install({Dio? dio}) async {
    if (dio != null &&
        !dio.interceptors.any((item) => item is HttpDeckDioInterceptor)) {
      dio.interceptors.add(HttpDeckDioInterceptor());
    }
  }

  static void captureRequest(RequestOptions options) {
    if (!captureEnabled) return;
    try {
      options.extra['_httpdeckStartMs'] =
          DateTime.now().millisecondsSinceEpoch;
    } catch (_) {
      // Never break the business request.
    }
  }

  static void captureResponse(Response response) {
    if (!captureEnabled) return;
    try {
      final options = response.requestOptions;
      _send({
        'method': options.method,
        'url': options.uri.toString(),
        'requestHeaders': _stringMap(options.headers),
        'requestBody': _bodyOf(options),
        'statusCode': response.statusCode,
        'responseHeaders': _headerMap(response.headers.map),
        'responseBody': _stringify(response.data),
        'durationMs': _durationOf(options),
        'timestamp': DateTime.now().millisecondsSinceEpoch,
        'replayed': options.extra['_httpdeckReplayed'] == true,
      });
    } catch (_) {
      // Capture must never break the response.
    }
  }

  static void captureError(DioError err) {
    if (!captureEnabled) return;
    try {
      final options = err.requestOptions;
      _send({
        'method': options.method,
        'url': options.uri.toString(),
        'requestHeaders': _stringMap(options.headers),
        'requestBody': _bodyOf(options),
        'statusCode': err.response?.statusCode,
        'responseHeaders': _headerMap(err.response?.headers.map ?? {}),
        'responseBody': _stringify(err.response?.data),
        'durationMs': _durationOf(options),
        'error': err.message,
        'timestamp': DateTime.now().millisecondsSinceEpoch,
        'replayed': options.extra['_httpdeckReplayed'] == true,
      });
    } catch (_) {
      // Capture must never break the error path.
    }
  }

  static void _send(Map<String, dynamic> payload) {
    if (!captureEnabled) return;
    unawaited(
      _channel
          .invokeMethod<void>('debugNetworkEvent', payload)
          .catchError((Object _) {}),
    );
  }

  static String? _bodyOf(RequestOptions options) {
    final debugBody = options.extra['_httpdeckBody'];
    if (debugBody is String) return _truncate(debugBody);
    final data = options.data;
    if (data == null) return null;
    if (data is Stream || data is FormData) return '<stream/form-data body>';
    if (data is String) return _truncate(data);
    if (data is Map || data is List) {
      try {
        return _truncate(jsonEncode(data));
      } catch (_) {
        return null;
      }
    }
    return null;
  }

  static String? _stringify(dynamic data) {
    if (data == null) return null;
    if (data is String) return _truncate(data);
    if (data is Map || data is List) {
      try {
        return _truncate(jsonEncode(data));
      } catch (_) {
        return null;
      }
    }
    return null;
  }

  static String _truncate(String text) {
    if (text.length <= _maxBodyChars) return text;
    return '${text.substring(0, _maxBodyChars)}'
        '\n...[truncated ${text.length - _maxBodyChars} chars]';
  }

  static Map<String, String> _stringMap(Map<String, dynamic> source) {
    final result = <String, String>{};
    source.forEach((key, value) {
      if (value != null) result[key] = value.toString();
    });
    return result;
  }

  static Map<String, String> _headerMap(Map<String, List<String>> source) {
    final result = <String, String>{};
    source.forEach((key, values) {
      result[key] = values.join(', ');
    });
    return result;
  }

  static int _durationOf(RequestOptions options) {
    final start = options.extra['_httpdeckStartMs'];
    if (start is int) {
      return DateTime.now().millisecondsSinceEpoch - start;
    }
    return 0;
  }
}

class HttpDeckDioInterceptor extends Interceptor {
  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    HttpDeckFlutter.captureRequest(options);
    handler.next(options);
  }

  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) {
    HttpDeckFlutter.captureResponse(response);
    handler.next(response);
  }

  @override
  void onError(DioError err, ErrorInterceptorHandler handler) {
    HttpDeckFlutter.captureError(err);
    handler.next(err);
  }
}