DESCRIPTION = "OpenXT status-report and status-tool"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-2.0;md5=801f80980d171dd6425610833a22dbe6"

PR = "r1"

SRC_URI = "file://status-report \
	   file://status-tool \
"

FILES_${PN} = "/"

do_install () {
	install -d ${D}/usr/bin
	install -m 0755 ${WORKDIR}/status-report \
		${D}/usr/bin/status-report
	install -m 0755 ${WORKDIR}/status-tool \
		${D}/usr/bin/status-tool
}
