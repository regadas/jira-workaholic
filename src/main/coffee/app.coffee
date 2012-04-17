issue = "{{#issues}}<li>
    <span class='label label-info issue-event' data-issue='{{ key }}' data-summary='{{ summary }}'>{{ key }}</span>
    <p class='issue-text' data-original-title='{{summary}}'>
      <small>{{summary}}</small>
    </p>
  </li>{{/issues}}"
info = "<div class='alert alert-info'>{{ message }}</div>"
warn_sync = "<div class='alert'>
    <strong> Warning! </strong>{{ message }}<a class='btn btn-warning' href='/user/state/sync'>Sync with JIRA</a>
  </div>"
issue_template = Hogan.compile(issue)
info_message = Hogan.compile(info)
warn_sync_message = Hogan.compile(warn_sync)
calendar = $('#calendar')

$.ajaxSetup
  error: 
    (jqXHR, exception) ->
      console.log jqXHR.status
      window.location = '/login' if jqXHR.status == 403 #forbidden

render_calendar = (events) ->
  calendar.empty()
  calendar.fullCalendar
      events: events
      header:
          left: 'prev,next today'
          center: 'title'
          right: 'month,agendaWeek,agendaDay'
      allDayDefault: false
      editable: true
      droppable: true
      eventDrop: (event,dayDelta,minuteDelta,allDay,revertFunc) ->
        console.log(
            event.title + " was moved " +
            dayDelta + " days and " +
            minuteDelta + " minutes."
        )
        if allDay
          console.log "Event is now all-day"
        else
          console.log "Event has a time-of-day"
        unless confirm("Are you sure about this change?")
          revertFunc()
      eventClick: (event, jsEvent, view) ->
        #TODO: confirm dialog should be temporary
        if confirm("Delete this worklog?")
          $.post "/issues/#{event.issue}/worklog/delete", JSON.stringify({
            created: event.created
          }), (data) ->
            calendar.fullCalendar 'removeEvents', (object) ->
              object.created == event.created
      drop: (date, allDay) ->
          original = $(this).data 'eventObject'
          copy = $.extend {}, original
          copy.start = date
          copy.end = new Date(date)
          copy.allDay = false
          $('#myModal').data 'eventObject', copy
          $('#myModal').modal 'toggle'
          $('#notification').show()

render_events = (unsaved, events...) ->
  events.map (event) ->
    event.color = 'red' if unsaved
    calendar.fullCalendar 'renderEvent', event, true

make_droppable = (elem) ->
  project = elem.data('project')
  issue = elem.data('issue')
  elem.data 'eventObject', 
    title: $.trim issue
    issue: issue
    project: project
  elem.draggable
    zIndex: 999,
    revert: true,
    revertDuration: 0

check_state = () ->
  $.getJSON "/user/state", (data) ->
    if data.state
      $('#messages').append warn_sync_message.render
        message: "Hey! You have unsaved changes."

user_issues = (issue) ->
  $.getJSON "/user/issues", (data) ->
    rendered = $(issue_template.render issues: data)
    $('#user-issues').append rendered
    data.map (i) ->
      $.getJSON "/issues/#{i.key}/worklogs", (data) ->
        render_events false, data...
    make_droppable rendered.find '.issue-event'

user_add_issue = (key) ->
  $.post "/user/issues", JSON.stringify({'key':key}), (data) ->
    $.getJSON "/issues/#{key}/worklogs", (data) ->
      render_events false, data...

#issues from the favorite area made droppable
$('#user-issues').droppable
  drop: (event, ui) ->
    issue_dom = ui.draggable
    $(this).find('.empty').remove()
    elems = $(this).find 'li .issue-event'
    exists = () ->
      result = false
      elems.each (e) ->
        if $(this).data('issue') is issue_dom.data('issue')
          result = true
      result
    unless exists()
      rendered = $(issue_template.render
        issues:
          key: issue_dom.data 'issue'
          summary: issue_dom.data 'summary'
      )
      make_droppable rendered.find '.issue-event'
      $(this).append rendered
      user_add_issue issue_dom.data('issue')

$('.issue-text').live 'hover', (e) ->
  $(this).tooltip
    placement: 'right'
  $(this).tooltip 'show'

$('.project').live 'click', (e) ->
  p = $(this)
  if !p.hasClass 'active'
    $('.active').find('ul').hide()
    $('.active').removeClass 'active'
    p.addClass 'active'
    ul = $(this).find 'ul'
    project = $(this).data 'key'
    $.getJSON "/projects/#{project}/issues", (data) ->
      data.map (e) ->
        e.project = project
      ul.html(issue_template.render({ issues: data })).show()
      $('.issue-event').each () ->
        make_droppable $(this)

    $.getJSON "/user/state", (data) ->
      render_events false, data.worklogs...
      $.getJSON "/projects/#{project}/worklogs", (data) ->
        calendar.fullCalendar 'removeEvents'
        render_events false, data...

# Consider using some sort of plugin here
typeTimeout = null
search = () ->
  q = $.trim $("#q").val()
  ul = $('#results').find('ul')
  ul.empty()
  if q.length > 4
    $('#search-spinner').spin 'small'
    $.post "/search/issues", {query: q }, (data) ->
      ul.empty()
      ul.append issue_template.render({ issues: data })
      ul.find('li .issue-event').each () ->
        make_droppable $(this)
      $('#search-spinner').spin false

$("#q").keyup (e) ->
  typeTimeout = setTimeout search, 500

$("#q").keydown (e) ->
  clearTimeout typeTimeout

$("#q-form").live 'submit', (e) ->
  e.preventDefault()
  clearTimeout typeTimeout
  search
  false

$('#add').live 'click', (e) ->
  event = $("#myModal").data 'eventObject'
  event.color = 'red'
  event.created = new Date()
  start = $("#start-event").val().split ':'
  end = $("#end-event").val().split ':'
  event.start.setHours start[0]
  event.start.setMinutes start[1]
  event.end.setHours end[0]
  event.end.setMinutes end[1]
  
  if event.end < event.start
    event.end.setDate(event.start.getDate() + 1)
  
  $.post "/issues/#{event.issue}/worklogs", JSON.stringify({
    start: event.start.getTime()
    end: event.end.getTime()
    created: event.created.getTime()
  }), (data) ->
    calendar.fullCalendar 'renderEvent', event, true
    check_state()
    
  $('#myModal').modal 'toggle'

$("#start-event").timepicker
  timeFormat: 'G:i'
  scrollDefaultNow: true
  step: 15
  
$("#end-event").timepicker
  timeFormat: 'G:i'
  scrollDefaultNow: true
  step: 15

render_calendar []
check_state()
user_issues()
