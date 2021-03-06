<?cs 
def:fullpage() ?>
  <div id="body-content">
<?cs /def ?>

<?cs 
def:guide_nav() ?>
  <div class="wrap clearfix" id="body-content">
    <div class="col-4" id="side-nav" itemscope itemtype="http://schema.org/SiteNavigationElement">
      <div id="devdoc-nav" class="scroll-pane">
<a class="totop" href="#top" data-g-event="left-nav-top">to top</a>


<?cs 
        include:"../../docs/guide/guide_toc.cs" ?>
        

      </div>
    </div> <!-- end side-nav -->
    <script>
      $(document).ready(function() {
        scrollIntoView("devdoc-nav");
        });
    </script>
<?cs /def ?>

<?cs # The default side navigation for the reference docs ?><?cs 
def:default_left_nav() ?>
<?cs if:reference.gcm || reference.gms ?>
  <?cs call:google_nav() ?>
<?cs else ?>
  <div class="wrap clearfix" id="body-content">
    <div class="col-4" id="side-nav" itemscope itemtype="http://schema.org/SiteNavigationElement">
      <div id="devdoc-nav">

<a class="totop" href="#top" data-g-event="left-nav-top">to top</a>
      <div id="api-nav-header">
        <div id="api-level-toggle">
          <label for="apiLevelCheckbox" class="disabled">API level: </label>
          <div class="select-wrapper">
            <select id="apiLevelSelector">
              <!-- option elements added by buildApiLevelSelector() -->
            </select>
          </div>
        </div><!-- end toggle -->
        <div id="api-nav-title">Android APIs</div>
        </div><!-- end nav header -->
      <script>
        var SINCE_DATA = [ <?cs 
          each:since = since ?>'<?cs 
            var:since.name ?>'<?cs 
            if:!last(since) ?>, <?cs /if ?><?cs
          /each 
        ?> ];
        buildApiLevelSelector();
      </script>
                  
      <div id="swapper">
        <div id="nav-panels">
          <div id="resize-packages-nav">
            <div id="packages-nav" class="scroll-pane">

              <ul>
              	<?cs call:package_link_list(docs.packages) ?>
              </ul><br/>

            </div> <!-- end packages-nav -->
          </div> <!-- end resize-packages -->
          <div id="classes-nav" class="scroll-pane">


<?cs 
            if:subcount(class.package) ?>
            <ul>
              <?cs call:list("Interfaces", class.package.interfaces) ?>
              <?cs call:list("Classes", class.package.classes) ?>
              <?cs call:list("Enums", class.package.enums) ?>
              <?cs call:list("Exceptions", class.package.exceptions) ?>
              <?cs call:list("Errors", class.package.errors) ?>
            </ul><?cs 
            elif:subcount(package) ?>
            <ul>
              <?cs call:class_link_list("Interfaces", package.interfaces) ?>
              <?cs call:class_link_list("Classes", package.classes) ?>
              <?cs call:class_link_list("Enums", package.enums) ?>
              <?cs call:class_link_list("Exceptions", package.exceptions) ?>
              <?cs call:class_link_list("Errors", package.errors) ?>
            </ul><?cs 
            else ?>
              <p style="padding:10px">Select a package to view its members</p><?cs 
            /if ?><br/>
        

          </div><!-- end classes -->
        </div><!-- end nav-panels -->
        <div id="nav-tree" style="display:none" class="scroll-pane">
          <div id="tree-list"></div>
        </div><!-- end nav-tree -->
      </div><!-- end swapper -->
      <div id="nav-swap">
      <a class="fullscreen">fullscreen</a>
      <a href='#' onclick='swapNav();return false;'><span id='tree-link'>Use Tree Navigation</span><span id='panel-link' style='display:none'>Use Panel Navigation</span></a>
      </div>
    </div> <!-- end devdoc-nav -->
    </div> <!-- end side-nav -->
    <script type="text/javascript">
      // init fullscreen based on user pref
      var fullscreen = readCookie("fullscreen");
      if (fullscreen != 0) {
        if (fullscreen == "false") {
          toggleFullscreen(false);
        } else {
          toggleFullscreen(true);
        }
      }
      // init nav version for mobile
      if (isMobile) {
        swapNav(); // tree view should be used on mobile
        $('#nav-swap').hide();
      } else {
        chooseDefaultNav();
        if ($("#nav-tree").is(':visible')) {
          init_default_navtree("<?cs var:toroot ?>");
        }
      }
      // scroll the selected page into view
      $(document).ready(function() {
        scrollIntoView("packages-nav");
        scrollIntoView("classes-nav");
        });
    </script>
<?cs /if ?>
    <?cs 
/def ?>

<?cs 
def:custom_left_nav() ?><?cs
  if:fullpage ?><?cs 
    call:fullpage() ?><?cs 
  elif:guide ?><?cs 
    call:guide_nav() ?><?cs   
  else ?><?cs 
    call:default_left_nav() ?> <?cs 
  /if ?><?cs 
/def ?>

<?cs 
def:custom_copyright() ?>
  Copyright (C) 2016 <a
  href="http://esrlabs.com">E.S.R.Labs</a>.<?cs 
/def ?>

<?cs 
def:custom_footerlinks() ?>
  <?cs 
/def ?>

<?cs # appears on the right side of the blue bar at the bottom off every page ?><?cs 
def:custom_buildinfo() ?>
  <?cs
/def ?>

