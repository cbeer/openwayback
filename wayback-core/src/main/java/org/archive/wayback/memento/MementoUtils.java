package org.archive.wayback.memento;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import org.archive.wayback.core.CaptureSearchResult;
import org.archive.wayback.core.CaptureSearchResults;
import org.archive.wayback.core.WaybackRequest;
import org.archive.wayback.partition.NotableResultExtractor;
import org.archive.wayback.util.ObjectFilterIterator;
import org.archive.wayback.util.StringFormatter;
import org.archive.wayback.webapp.AccessPoint;

public class MementoUtils implements MementoConstants {

	public final static SimpleDateFormat HTTP_LINK_DATE_FORMATTER;
	public final static SimpleDateFormat DATE_FORMAT_14_FORMATTER;

	static {
		HTTP_LINK_DATE_FORMATTER = new SimpleDateFormat(HTTP_LINK_DATE_FORMAT, Locale.ENGLISH);
		HTTP_LINK_DATE_FORMATTER.setTimeZone(GMT_TZ);
		DATE_FORMAT_14_FORMATTER = new SimpleDateFormat(DATE_FORMAT_14, Locale.ENGLISH);
		DATE_FORMAT_14_FORMATTER.setTimeZone(GMT_TZ);
	}
	
	public static void printTimemapResponse(CaptureSearchResults results, WaybackRequest wbRequest, HttpServletResponse response) throws IOException
    {
		response.setContentType("application/link-format");
		printLinkTimemap(results, wbRequest, response.getWriter());
	}

	public static void printLinkTimemap(CaptureSearchResults results,
			WaybackRequest wbr, PrintWriter pw) {
		Date first = results.getFirstResultDate();
		Date last = results.getLastResultDate();
		AccessPoint ap = wbr.getAccessPoint();

		String requestUrl = wbr.getRequestUrl();
		// ludab nov30 2012

		String pagedate = wbr.get(PAGE_STARTS);
		if (pagedate == null) {
			pagedate = "";
		} else {
			pagedate = pagedate + "/";
		}
		// end

		pw.print(makeLink(requestUrl, ORIGINAL));
		pw.println(",");
		// ludab nov 30 2012

		// pw.print(makeLink(getTimemapUrl(ap,FORMAT_LINK,requestUrl),
		// TIMEMAP,APPLICATION_LINK_FORMAT));
		pw.print(makeLink(
				getTimemapDateUrl(ap, FORMAT_LINK, pagedate, requestUrl),
				"self", APPLICATION_LINK_FORMAT)
				+ "; from=\""
				+ HTTP_LINK_DATE_FORMATTER.format(first)
				+ "\""
				+ "; until=\""
				+ HTTP_LINK_DATE_FORMATTER.format(last) + "\"");
		// end
		pw.println(",");
		pw.print(makeLink(getTimegateUrl(ap, requestUrl), TIMEGATE));
		pw.println(",");

		if (first.compareTo(last) == 0) {
			// special handling of single result:
			CaptureSearchResult result = results.getResults().get(0);
			pw.print(makeLink(ap, result.getOriginalUrl(), FIRST_LAST_MEMENTO, result));
		} else {
			List<CaptureSearchResult> lr = results.getResults();
			int count = lr.size();
			String rel;
			for (int i = 0; i < count; i++) {
				CaptureSearchResult result = lr.get(i);
				if (i == 0) {
					rel = FIRST_MEMENTO;
				} else if (i == count - 1) {
					pw.println(",");
					rel = LAST_MEMENTO;
				} else {
					pw.println(",");
					rel = MEMENTO;
				}
				pw.print(makeLink(ap, result.getOriginalUrl(), rel,
						result));
			}
		}
		// ludab nov 30 2012
		if (results.getMatchingCount() > results.getReturnedCount()) {
			int sec = last.getSeconds() + 1;
			last.setSeconds(sec);
			pw.println(",");
			pw.print(makeLink(
					getTimemapDateUrl(ap, FORMAT_LINK,
							DATE_FORMAT_14_FORMATTER.format(last) + "/",
							requestUrl), TIMEMAP, APPLICATION_LINK_FORMAT)
					+ "; from=\""
					+ HTTP_LINK_DATE_FORMATTER.format(last)
					+ "\"");
		}
		// end

		pw.flush();
	}

	public static String generateMementoLinkHeaders(
			CaptureSearchResults results, WaybackRequest wbr, boolean includeTimegateLink, boolean includeOriginalLink) {
		NotableResultExtractor nre = getNotableResults(results);
		CaptureSearchResult first = nre.getFirst();
		CaptureSearchResult prev = nre.getPrev();
		CaptureSearchResult closest = nre.getClosest();
		CaptureSearchResult next = nre.getNext();
		CaptureSearchResult last = nre.getLast();
		ArrayList<String> rels = new ArrayList<String>();

		AccessPoint ap = wbr.getAccessPoint();

		String requestUrl = wbr.getRequestUrl();

		// add generics:
		// rels.add(makeLink(getTimebundleUrl(ap, requestUrl), TIMEBUNDLE));
		if (includeOriginalLink) {
		    rels.add(makeLink(requestUrl, ORIGINAL));
		}

		rels.add(makeLink(getTimemapUrl(ap, FORMAT_LINK, requestUrl), TIMEMAP,
				APPLICATION_LINK_FORMAT));
		
		// Spec says not to include timegate link for timegate
		if (includeTimegateLink) {
			rels.add(makeLink(getTimegateUrl(ap, requestUrl), TIMEGATE));
		}

		// add first/prev/next/last:
		if (first == last) {
			// only one capture.. are we sure we want the "actual" memento here?
			rels.add(makeLink(ap, requestUrl, FIRST_LAST_MEMENTO,
					first));
		} else {
			if (first == closest) {
				// no previous:
				rels.add(makeLink(ap, requestUrl, FIRST_MEMENTO,
						first));
				if (next == last) {
					rels.add(makeLink(ap, requestUrl, NEXT_LAST_MEMENTO,
							last));
				} else {
					rels.add(makeLink(ap, requestUrl, NEXT_MEMENTO,
							next));
					rels.add(makeLink(ap, requestUrl, LAST_MEMENTO,
							last));
				}
			} else if (last == closest) {
				// no next:
				rels.add(makeLink(ap, requestUrl, LAST_MEMENTO,
						last));
				if (prev == first) {
					rels.add(makeLink(ap, requestUrl, PREV_FIRST_MEMENTO,
							first));
				} else {
					rels.add(makeLink(ap, requestUrl, FIRST_MEMENTO,
							first));
					rels.add(makeLink(ap, requestUrl, PREV_MEMENTO,
							prev));
				}
			} else {
				// somewhere in the middle:

				if (prev == first) {
					rels.add(makeLink(ap, requestUrl, PREV_FIRST_MEMENTO,
							first));
				} else {
					// add both prev and first:
					rels.add(makeLink(ap, requestUrl, FIRST_MEMENTO,
							first));
					rels.add(makeLink(ap, requestUrl, PREV_MEMENTO,
							prev));
				}
				// add "actual" memento:
				rels.add(makeLink(ap, requestUrl, MEMENTO,
						closest));
				if (next == last) {
					rels.add(makeLink(ap, requestUrl, NEXT_LAST_MEMENTO,
							last));
				} else {
					rels.add(makeLink(ap, requestUrl, NEXT_MEMENTO,
							next));
					rels.add(makeLink(ap, requestUrl, LAST_MEMENTO,
							last));
				}
			}
		}
		return StringFormatter.join(", ", rels.toArray(a));
	}

	static String[] a = new String[0];

	public static void addVaryHeader(HttpServletResponse response) {
		response.setHeader(VARY, NEGOTIATE_DATETIME);
	}

	public static boolean hasLinkHeader(HttpServletResponse response) {
		// HRM.. Are we sure it's *our* Link header, and has the rel="original"?
		return response.containsHeader(LINK);
	}

	public static void addOrigHeader(HttpServletResponse response, String url) {
		response.setHeader(LINK, makeLink(url, ORIGINAL));
	}
	
	public static void addDoNotNegotiateHeader(HttpServletResponse response) {
		// New Non-Negotiate header
		// Link: <http://mementoweb.org/terms/donotnegotiate">; rel="type" 
		response.setHeader(LINK, makeLink("http://mementoweb.org/terms/donotnegotiate", "type"));
	}

	public static void addOrigHeader(HttpServletResponse response,
			WaybackRequest wbr) {
		addOrigHeader(response, wbr.getRequestUrl());
	}
	
	public static String makeOrigHeader(String url)
	{
		return makeLink(url, ORIGINAL);
	}

	public static void addMementoHeaders(HttpServletResponse response,
			CaptureSearchResults results, CaptureSearchResult result, WaybackRequest wbr) {
		response.setHeader(MEMENTO_DATETIME, HTTP_LINK_DATE_FORMATTER
				.format(results.getClosest().getCaptureDate()));
		
		if (!wbr.isMementoTimegate()) {
		    response.setHeader(LINK, generateMementoLinkHeaders(results, wbr, true, true));
		}
	}

	public static void addTimegateHeaders(HttpServletResponse response,
			CaptureSearchResults results, WaybackRequest wbr, boolean includeOriginal) {
		addVaryHeader(response);

		response.setHeader(LINK, generateMementoLinkHeaders(results, wbr, false, includeOriginal));
	}

//	private static String getTimegatePrefix(AccessPoint ap) {
//		// if(ap.getClass().isAssignableFrom(MementoAccessPoint.class)) {
//		String prefix = null;
//		if (ap instanceof MementoAccessPoint) {
//			prefix = ((MementoAccessPoint) ap).getTimegatePrefix();
//		}
//		if (prefix == null) {
//			prefix = getProp(ap.getConfigs(), TIMEGATE_PREFIX_CONFIG, null);
//		}
//		// TODO: rationalize...
//		if (prefix == null) {
//			prefix = ap.getReplayPrefix();
//		}
//		return prefix;
//	}

	public static final SimpleDateFormat ACCEPT_DATE_FORMATS[] = {
			new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
			new SimpleDateFormat("E, dd MMM yyyy Z", Locale.ENGLISH),
			new SimpleDateFormat("E, dd MMM yyyy", Locale.ENGLISH) };

	public static Date parseAcceptDateTimeHeader(String datespec) {
		for (SimpleDateFormat format : ACCEPT_DATE_FORMATS) {
			try {
				return format.parse(datespec);
			} catch (ParseException e) {
				// ignore and move on..
			}
		}
		
		return null;
	}

	public static String getTimegateUrl(AccessPoint ap, String url) {
		StringBuilder sb = new StringBuilder();
		sb.append(getTimeGatePrefix(ap));
		sb.append(url);
		return sb.toString();
	}

	public static String getTimemapUrl(AccessPoint ap, String format, String url) {
		StringBuilder sb = new StringBuilder();
		sb.append(getTimeMapPrefix(ap));
		sb.append(TIMEMAP).append("/").append(format).append("/");
		sb.append(url);
		return sb.toString();
	}

	public static String getTimemapDateUrl(AccessPoint ap, String format,
			String pagestr, String url) {
		StringBuilder sb = new StringBuilder();
		sb.append(getTimeMapPrefix(ap));
		sb.append(TIMEMAP).append("/").append(format).append("/");
		sb.append(pagestr);
		sb.append(url);
		return sb.toString();
	}

	public static String getTimeMapPrefix(AccessPoint ap) {
		return getMementoPrefix(ap) + ap.getQueryPrefix();
	}

	public static String getTimeGatePrefix(AccessPoint ap) {
		return getMementoPrefix(ap) + ap.getReplayPrefix();
	}

	public static String getMementoPrefix(AccessPoint ap) {
		return getProp(ap.getConfigs(), AGGREGATION_PREFIX_CONFIG, "");
		
//		String prefix = null;
//		if (ap instanceof MementoAccessPoint) {
//			prefix = ((MementoAccessPoint) ap).getTimegatePrefix();
//		}
//		// TODO: rationalize...
//		if (prefix == null) {
//			prefix = getProp(ap.getConfigs(), AGGREGATION_PREFIX_CONFIG, "");
//		}
//		if (prefix == null) {
//			prefix = ap.getQueryPrefix();
//		}
//		return prefix;
	}

	public static int getPageMaxRecord(AccessPoint ap) {
		String mr;
		mr = getProp(ap.getConfigs(), PAGE_MAXRECORDS_CONFIG, "0");
		if (mr == null) {
			mr = "0";
		}
		return new Integer(mr).intValue();
	}

	private static String getProp(Properties p, String name, String deflt) {
		if (p == null) {
			return deflt;
		}
		return p.getProperty(name, deflt);
	}

	private static String makeLink(String url, String rel) {
		return String.format("<%s>; rel=\"%s\"", url, rel);
	}

	private static String makeLink(String url, String rel, String type) {
		return String.format("<%s>; rel=\"%s\"; type=\"%s\"", url, rel, type);
	}

	private static String makeLink(AccessPoint ap, String url, String rel, CaptureSearchResult result) {

		Date date = result.getCaptureDate();
		String timestamp = DATE_FORMAT_14_FORMATTER.format(date);
		String replayURI = ap.getUriConverter().makeReplayURI(timestamp, url);
		String prefix = getMementoPrefix(ap);
		String httpTime = HTTP_LINK_DATE_FORMATTER.format(date);

//		return String.format("<%s%s>; rel=\"%s\"; datetime=\"%s\"; status=\"%s\"", prefix, replayURI,
//				rel, httpTime, result.getHttpCode());
		return String.format("<%s%s>; rel=\"%s\"; datetime=\"%s\"", prefix, replayURI, rel, httpTime);
	}

	private static NotableResultExtractor getNotableResults(
			CaptureSearchResults r) {
		// eventually, the NotableResultExtractor will be part of the standard
		// ResourceIndex.query() but for now, we'll just do an extra traversal
		// of the whole set of results:

		Iterator<CaptureSearchResult> itr = r.iterator();
		Date want = r.getClosest().getCaptureDate();
		NotableResultExtractor nre = new NotableResultExtractor(want);

		ObjectFilterIterator<CaptureSearchResult> ofi = new ObjectFilterIterator<CaptureSearchResult>(
				itr, nre);
		while (ofi.hasNext()) {
			ofi.next();
		}
		return nre;
	}
}
