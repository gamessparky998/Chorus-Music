document.addEventListener('DOMContentLoaded', () => {
    const observerOptions = {
        root: null,
        rootMargin: '0px',
        threshold: 0.1
    };

    const observer = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('visible');
                observer.unobserve(entry.target);
            }
        });
    }, observerOptions);

    const elementsToAnimate = document.querySelectorAll('.fade-in-scroll');
    elementsToAnimate.forEach(el => observer.observe(el));

    // Fetch latest release APK
    const downloadBtn = document.getElementById('download-btn');
    if (downloadBtn) {
        fetch('https://api.github.com/repos/slice-of-fun/Chorus-Music/releases/latest')
            .then(response => response.json())
            .then(data => {
                if (data && data.assets) {
                    const apkAsset = data.assets.find(asset => asset.name.endsWith('.apk'));
                    if (apkAsset) {
                        downloadBtn.href = apkAsset.browser_download_url;
                    }
                }
            })
            .catch(error => console.error('Error fetching latest release:', error));
    }
});
